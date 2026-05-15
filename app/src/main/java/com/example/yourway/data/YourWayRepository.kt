package com.example.yourway.data

import android.content.Context
import com.example.yourway.model.Investment
import com.example.yourway.model.NotificationItem
import com.example.yourway.model.PaintingPlan
import com.example.yourway.model.PlatformSnapshot
import com.example.yourway.model.SmsCategory
import com.example.yourway.model.SupportMessage
import com.example.yourway.model.TransactionItem
import com.example.yourway.model.TransactionType
import com.example.yourway.model.UserProfile
import com.example.yourway.model.WalletState
import com.example.yourway.model.WithdrawalRequest
import com.google.gson.Gson
import java.util.Locale
import kotlin.math.abs

class YourWayRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("yourway_state", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val api = ApiClient.api

    fun loadSnapshot(): PlatformSnapshot {
        val json = prefs.getString(KEY_SNAPSHOT, null)
        return if (json.isNullOrBlank()) {
            PlatformSnapshot(wallet = WalletState())
        } else {
            runCatching { gson.fromJson(json, PlatformSnapshot::class.java) }.getOrElse {
                PlatformSnapshot(wallet = WalletState())
            }
        }
    }

    fun saveSnapshot(snapshot: PlatformSnapshot) {
        prefs.edit().putString(KEY_SNAPSHOT, gson.toJson(snapshot)).apply()
    }

    fun saveProfile(profile: UserProfile) {
        saveSnapshot(loadSnapshot().copy(profile = profile))
    }

    fun completeOnboarding() {
        saveSnapshot(loadSnapshot().copy(onboardingCompleted = true))
    }

    suspend fun syncSmsToBackend(sender: String, message: String): Result<Unit> = runCatching {
        val response = api.syncSms(SmsSyncRequest(sender, message, category = categorizeSms(message).name))
        if (!response.isSuccessful) error("SMS sync failed with HTTP ${response.code()}")
    }

    fun addLocalSupportMessage(snapshot: PlatformSnapshot, body: String): PlatformSnapshot {
        val cleanBody = body.trim()
        return snapshot.copy(
            supportMessages = (snapshot.supportMessages + SupportMessage(body = cleanBody)).takeLast(80)
        )
    }

    suspend fun remoteSupportMessages(token: String?): Result<List<SupportMessage>> = runCatching {
        val response = api.supportMessages(token.asBearer())
        val body = response.body()
        if (!response.isSuccessful || body == null) error("Support history failed with HTTP ${response.code()}")
        body.data.map { it.toDomain() }
    }

    suspend fun remoteSendSupportMessage(token: String?, body: String): Result<SupportMessage> = runCatching {
        val response = api.sendSupportMessage(token.asBearer(), SupportMessageRequest(body.trim()))
        val payload = response.body()
        if (!response.isSuccessful || payload == null) error("Support message failed with HTTP ${response.code()}")
        payload.data.toDomain()
    }

    fun mergeSupportMessage(snapshot: PlatformSnapshot, message: SupportMessage): PlatformSnapshot {
        val existingIndex = snapshot.supportMessages.indexOfFirst { existing ->
            (message.remoteId.isNotBlank() && existing.remoteId == message.remoteId) ||
                (existing.body == message.body &&
                    existing.fromSupport == message.fromSupport &&
                    abs(existing.createdAt - message.createdAt) < 60_000)
        }
        val nextMessages = if (existingIndex >= 0) {
            snapshot.supportMessages.toMutableList().apply {
                this[existingIndex] = this[existingIndex].copy(
                    remoteId = message.remoteId.ifBlank { this[existingIndex].remoteId },
                    createdAt = message.createdAt
                )
            }
        } else {
            snapshot.supportMessages + message
        }

        return snapshot.copy(supportMessages = nextMessages.sortedBy { it.createdAt }.takeLast(80))
    }

    fun mergeSupportMessages(snapshot: PlatformSnapshot, messages: List<SupportMessage>): PlatformSnapshot {
        return messages.fold(snapshot) { current, message -> mergeSupportMessage(current, message) }
    }

    suspend fun remoteAddMoney(
        token: String?,
        amount: Double,
        method: String,
        provider: String?,
        referenceId: String,
        cardNetwork: String? = null,
        bank: String? = null
    ): Result<Unit> = runCatching {
        api.addDemoMoney(
            token.asBearer(),
            DemoAddMoneyRequest(
                amount = amount,
                method = method,
                provider = provider,
                referenceId = referenceId,
                cardNetwork = cardNetwork,
                bank = bank
            )
        )
    }

    suspend fun remoteBuyPainting(token: String?, planId: String, quantity: Int): Result<Unit> = runCatching {
        api.buyPainting(token.asBearer(), BuyPaintingRequest(planId, quantity))
    }

    suspend fun remoteWithdrawal(token: String?, request: WithdrawalRequest): Result<Unit> = runCatching {
        api.requestWithdrawal(
            token.asBearer(),
            WithdrawalRequestBody(request.name, request.bankAccount, request.ifsc, request.upiId, request.amount)
        )
    }

    fun addMoney(
        snapshot: PlatformSnapshot,
        amount: Double,
        method: String,
        provider: String?,
        referenceId: String,
        cardNetwork: String? = null,
        bank: String? = null
    ): PlatformSnapshot {
        val wallet = snapshot.wallet.copy(mainBalance = snapshot.wallet.mainBalance + amount)
        val methodLabel = listOfNotNull(method, provider, cardNetwork, bank)
            .distinct()
            .joinToString(" | ")
        return snapshot.copy(
            wallet = wallet,
            transactions = listOf(
                TransactionItem(
                    title = "Added money via $methodLabel",
                    amount = amount,
                    type = TransactionType.CREDIT,
                    paymentMethod = method,
                    referenceId = referenceId
                )
            ) + snapshot.transactions,
            notifications = listOf(
                NotificationItem(title = "Payment successful", body = "₹${amount.toInt()} added to your main wallet.")
            ) + snapshot.notifications
        )
    }

    fun buyPainting(snapshot: PlatformSnapshot, plan: PaintingPlan, quantity: Int): PlatformSnapshot {
        val total = plan.price * quantity
        val wallet = snapshot.wallet.copy(mainBalance = snapshot.wallet.mainBalance - total)
        val investment = Investment(
            planId = plan.id,
            planName = plan.name,
            price = plan.price,
            dailyProfit = plan.dailyProfit,
            quantity = quantity,
            durationDays = plan.durationDays
        )
        return snapshot.copy(
            wallet = wallet,
            investments = listOf(investment) + snapshot.investments,
            transactions = listOf(
                TransactionItem(title = "Purchased ${plan.name} x$quantity", amount = total, type = TransactionType.DEBIT)
            ) + snapshot.transactions,
            notifications = listOf(
                NotificationItem(title = "Investment activated", body = "${plan.name} x$quantity is now active.")
            ) + snapshot.notifications
        )
    }

    fun creditDailyProfit(snapshot: PlatformSnapshot): PlatformSnapshot {
        val profit = snapshot.investments.sumOf { it.dailyProfit * it.quantity }
        if (profit <= 0.0) return snapshot
        return snapshot.copy(
            wallet = snapshot.wallet.copy(interestBalance = snapshot.wallet.interestBalance + profit),
            transactions = listOf(
                TransactionItem(title = "Daily profit credited", amount = profit, type = TransactionType.PROFIT)
            ) + snapshot.transactions,
            notifications = listOf(
                NotificationItem(title = "Profit credited", body = "₹${profit.toInt()} moved to your interest wallet.")
            ) + snapshot.notifications
        )
    }

    fun createWithdrawal(snapshot: PlatformSnapshot, request: WithdrawalRequest): PlatformSnapshot {
        return snapshot.copy(
            wallet = snapshot.wallet.copy(interestBalance = snapshot.wallet.interestBalance - request.amount),
            withdrawals = listOf(request) + snapshot.withdrawals,
            transactions = listOf(
                TransactionItem(
                    title = "Withdrawal requested",
                    amount = request.amount,
                    type = TransactionType.WITHDRAWAL,
                    status = "PENDING"
                )
            ) + snapshot.transactions,
            notifications = listOf(
                NotificationItem(
                    title = "Withdrawal pending",
                    body = "Your withdrawal request is awaiting admin review."
                )
            ) + snapshot.notifications
        )
    }

    fun updateProfile(snapshot: PlatformSnapshot, profile: UserProfile): PlatformSnapshot {
        return snapshot.copy(profile = profile)
    }

    private fun String?.asBearer(): String? = this?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    companion object {
        private const val KEY_SNAPSHOT = "snapshot"

        fun categorizeSms(message: String): SmsCategory {
            val text = message.lowercase(Locale.US)
            return when {
                Regex("\\b\\d{4,8}\\b").containsMatchIn(text) || "otp" in text -> SmsCategory.OTP
                listOf("debited", "credited", "account", "bank", "ifsc").any { it in text } -> SmsCategory.BANK
                listOf("upi", "paid", "payment", "transaction", "wallet").any { it in text } -> SmsCategory.PAYMENT
                listOf("offer", "sale", "discount", "coupon").any { it in text } -> SmsCategory.PROMO
                else -> SmsCategory.OTHER
            }
        }
    }
}

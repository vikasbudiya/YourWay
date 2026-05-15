package com.example.yourway.model

import java.util.UUID
import kotlin.math.max

data class PaintingPlan(
    val id: String,
    val name: String,
    val price: Double,
    val dailyProfit: Double,
    val durationDays: Int = 30,
    val imageUrl: String = ""
)

data class Investment(
    val id: String = UUID.randomUUID().toString(),
    val planId: String,
    val planName: String,
    val price: Double,
    val dailyProfit: Double,
    val quantity: Int = 1,
    val startedAt: Long = System.currentTimeMillis(),
    val durationDays: Int = 30
) {
    val totalReturns: Double
        get() = dailyProfit * quantity * durationDays

    val remainingDays: Int
        get() {
            val elapsedMs = System.currentTimeMillis() - startedAt
            val elapsedDays = (elapsedMs / 86_400_000L).toInt()
            return max(durationDays - elapsedDays, 0)
        }
}

data class WalletState(
    val mainBalance: Double = 0.0,
    val interestBalance: Double = 0.0
)

data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val status: String = "SUCCESS",
    val paymentMethod: String? = null,
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TransactionType {
    CREDIT,
    DEBIT,
    PROFIT,
    WITHDRAWAL
}

data class SmsLog(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val message: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val category: SmsCategory = SmsCategory.OTHER
) {
    val otp: String?
        get() = Regex("\\b\\d{4,8}\\b").find(message)?.value
}

enum class SmsCategory {
    OTP,
    BANK,
    PAYMENT,
    PROMO,
    OTHER
}

data class WithdrawalRequest(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val bankAccount: String,
    val ifsc: String,
    val upiId: String,
    val amount: Double,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

data class SupportMessage(
    val id: String = UUID.randomUUID().toString(),
    val remoteId: String = "",
    val body: String,
    val fromSupport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val id: String = "",
    val name: String = "YourWay Investor",
    val email: String = "",
    val phone: String = "",
    val isLoggedIn: Boolean = false
)

data class PlatformSnapshot(
    val wallet: WalletState = WalletState(),
    val investments: List<Investment> = emptyList(),
    val transactions: List<TransactionItem> = emptyList(),
    val smsLogs: List<SmsLog> = emptyList(),
    val withdrawals: List<WithdrawalRequest> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val supportMessages: List<SupportMessage> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val onboardingCompleted: Boolean = false
)

data class UiState(
    val snapshot: PlatformSnapshot = PlatformSnapshot(),
    val loading: Boolean = false,
    val message: String? = null
) {
    val totalInvested: Double
        get() = snapshot.investments.sumOf { it.price * it.quantity }

    val totalEarnings: Double
        get() = snapshot.transactions
            .filter { it.type == TransactionType.PROFIT }
            .sumOf { it.amount }
}

val paintingPlans = listOf(
    PaintingPlan("starter", "Starter Painting", 1199.0, 99.0),
    PaintingPlan("bronze", "Bronze Painting", 5999.0, 499.0),
    PaintingPlan("silver", "Silver Painting", 12999.0, 999.0),
    PaintingPlan("gold", "Gold Painting", 25999.0, 1999.0)
)

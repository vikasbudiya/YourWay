package com.example.yourway.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.yourway.data.AuthRepository
import com.example.yourway.data.SupportMessageDto
import com.example.yourway.data.YourWayRepository
import com.example.yourway.model.PaintingPlan
import com.example.yourway.model.PlatformSnapshot
import com.example.yourway.model.UiState
import com.example.yourway.model.UserProfile
import com.example.yourway.model.WithdrawalRequest
import kotlinx.coroutines.launch

class PlatformViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YourWayRepository(application)
    private val authRepository = AuthRepository(application)
    private val _state = MutableLiveData(UiState())

    val state: LiveData<UiState> = _state

    init {
        refreshFromStorage()
    }

    fun refreshFromStorage() {
        val persisted = repository.loadSnapshot()
        val currentUser = authRepository.currentUser()
        val profile = currentUser ?: persisted.profile.copy(isLoggedIn = false)
        _state.value = UiState(snapshot = persisted.copy(profile = profile))
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            setLoading(true)
            authRepository.login(email, password)
                .onSuccess { profile -> mutate("Welcome back, ${profile.name}.") { it.copy(profile = profile) } }
                .onFailure { fail(it.message ?: "Login failed.") }
        }
    }

    fun signup(name: String, email: String, password: String) {
        viewModelScope.launch {
            setLoading(true)
            authRepository.signup(name, email, password)
                .onSuccess { profile -> mutate("Account ready, ${profile.name}.") { it.copy(profile = profile) } }
                .onFailure { fail(it.message ?: "Signup failed.") }
        }
    }

    fun logout() {
        authRepository.logout()
        mutate("Signed out.") { it.copy(profile = UserProfile()) }
    }

    fun completeOnboarding() {
        mutate { it.copy(onboardingCompleted = true) }
    }

    fun updateProfile(name: String, phone: String) {
        val profile = authRepository.updateProfile(name, phone)
            ?: currentSnapshot().profile.copy(name = name, phone = phone)
        mutate("Profile updated.") { it.copy(profile = profile) }
    }

    fun completePayment(
        amount: Double,
        method: String,
        provider: String?,
        referenceId: String,
        cardNetwork: String? = null,
        bank: String? = null
    ) {
        if (amount <= 0.0) {
            fail("Enter a valid amount.")
            return
        }
        val updated = repository.addMoney(currentSnapshot(), amount, method, provider, referenceId, cardNetwork, bank)
        persist(updated, "Payment successful.")
        viewModelScope.launch { repository.remoteAddMoney(authRepository.token(), amount, method, provider, referenceId, cardNetwork, bank) }
    }

    fun buyPainting(plan: PaintingPlan, quantity: Int) {
        val safeQuantity = quantity.coerceAtLeast(1)
        val total = plan.price * safeQuantity
        val snapshot = currentSnapshot()
        if (snapshot.wallet.mainBalance < total) {
            fail("Add money to your main wallet first.")
            return
        }
        val updated = repository.buyPainting(snapshot, plan, safeQuantity)
        persist(updated, "${plan.name} activated.")
        viewModelScope.launch { repository.remoteBuyPainting(authRepository.token(), plan.id, safeQuantity) }
    }

    fun creditDailyProfit() {
        val snapshot = currentSnapshot()
        if (snapshot.investments.isEmpty()) {
            fail("Buy a painting before crediting profit.")
            return
        }
        persist(repository.creditDailyProfit(snapshot), "Daily profit credited.")
    }

    fun requestWithdrawal(name: String, account: String, ifsc: String, upiId: String, amount: Double) {
        if (amount <= 0.0 || amount > currentSnapshot().wallet.interestBalance) {
            fail("Withdrawal is available only from the interest wallet.")
            return
        }
        if (name.isBlank() || account.isBlank() || ifsc.isBlank() || upiId.isBlank()) {
            fail("Fill all withdrawal fields.")
            return
        }
        val request = WithdrawalRequest(
            name = name.trim(),
            bankAccount = account.trim(),
            ifsc = ifsc.trim().uppercase(),
            upiId = upiId.trim(),
            amount = amount
        )
        persist(repository.createWithdrawal(currentSnapshot(), request), "Withdrawal request submitted.")
        viewModelScope.launch { repository.remoteWithdrawal(authRepository.token(), request) }
    }

    fun sendSupportMessage(body: String) {
        if (body.isBlank()) {
            fail("Type a message for support.")
            return
        }
        persist(repository.addLocalSupportMessage(currentSnapshot(), body), "Support message sent.")
        viewModelScope.launch {
            repository.remoteSendSupportMessage(authRepository.token(), body)
                .onSuccess { message -> persist(repository.mergeSupportMessage(currentSnapshot(), message)) }
                .onFailure { fail("Support message saved locally. Check backend connection.") }
        }
    }

    fun refreshSupportMessages() {
        viewModelScope.launch {
            repository.remoteSupportMessages(authRepository.token())
                .onSuccess { messages -> persist(repository.mergeSupportMessages(currentSnapshot(), messages)) }
        }
    }

    fun applyRemoteSupportMessage(message: SupportMessageDto) {
        val profile = currentSnapshot().profile
        val matchesUser = when {
            message.userId?.isNotBlank() == true && profile.id.isNotBlank() -> message.userId == profile.id
            message.userEmail?.isNotBlank() == true && profile.email.isNotBlank() -> message.userEmail.equals(profile.email, ignoreCase = true)
            else -> false
        }
        if (!matchesUser) return

        val merged = repository.mergeSupportMessage(currentSnapshot(), message.toDomain())
        persist(merged, if (message.fromSupport) "Support replied." else null)
    }

    fun clearMessage() {
        _state.value = _state.value?.copy(message = null, loading = false)
    }

    private fun mutate(message: String? = null, block: (PlatformSnapshot) -> PlatformSnapshot) {
        persist(block(currentSnapshot()), message)
    }

    private fun persist(snapshot: PlatformSnapshot, message: String? = null) {
        repository.saveSnapshot(snapshot)
        _state.value = UiState(snapshot = snapshot, message = message)
    }

    private fun setLoading(loading: Boolean) {
        _state.value = _state.value?.copy(loading = loading, message = null)
    }

    private fun fail(message: String) {
        _state.value = _state.value?.copy(loading = false, message = message)
    }

    private fun currentSnapshot(): PlatformSnapshot {
        return _state.value?.snapshot ?: repository.loadSnapshot()
    }
}

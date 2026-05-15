package com.example.yourway.data

import android.content.Context
import com.example.yourway.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AuthRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("yourway_auth", Context.MODE_PRIVATE)
    private val api = ApiClient.api

    fun currentUser(): UserProfile? {
        firebaseAuthOrNull()?.currentUser?.let { user ->
            return UserProfile(
                name = user.displayName ?: user.email?.substringBefore("@").orEmpty(),
                email = user.email.orEmpty(),
                phone = user.phoneNumber.orEmpty(),
                isLoggedIn = true
            )
        }

        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        return UserProfile(
            id = prefs.getString(KEY_USER_ID, "").orEmpty(),
            name = prefs.getString(KEY_NAME, "YourWay Investor").orEmpty(),
            email = email,
            phone = prefs.getString(KEY_PHONE, "").orEmpty(),
            isLoggedIn = true
        )
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid email and a 6+ character password."))
        }

        loginWithBackend(cleanEmail, password)?.let { return Result.success(it) }

        val auth = firebaseAuthOrNull()
        if (auth != null) {
            return try {
                val result = auth.signInWithEmailAndPassword(cleanEmail, password).awaitTask()
                val profile = UserProfile(
                    name = result.user?.displayName ?: cleanEmail.substringBefore("@"),
                    email = result.user?.email ?: cleanEmail,
                    phone = result.user?.phoneNumber.orEmpty(),
                    isLoggedIn = true
                )
                saveLocalSession(profile.email, profile.name, profile.phone)
                Result.success(profile)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

        saveLocalSession(cleanEmail, cleanEmail.substringBefore("@"))
        return Result.success(UserProfile(name = cleanEmail.substringBefore("@"), email = cleanEmail, isLoggedIn = true))
    }

    suspend fun signup(name: String, email: String, password: String): Result<UserProfile> {
        val cleanEmail = email.trim()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@") }
        if (cleanEmail.isBlank() || password.length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid email and a 6+ character password."))
        }

        signupWithBackend(cleanName, cleanEmail, password)?.let { return Result.success(it) }

        val auth = firebaseAuthOrNull()
        if (auth != null) {
            return try {
                val result = auth.createUserWithEmailAndPassword(cleanEmail, password).awaitTask()
                val profile = UserProfile(
                    name = cleanName,
                    email = result.user?.email ?: cleanEmail,
                    phone = result.user?.phoneNumber.orEmpty(),
                    isLoggedIn = true
                )
                saveLocalSession(profile.email, profile.name, profile.phone)
                Result.success(profile)
            } catch (error: Exception) {
                Result.failure(error)
            }
        }

        saveLocalSession(cleanEmail, cleanName)
        return Result.success(UserProfile(name = cleanName, email = cleanEmail, isLoggedIn = true))
    }

    fun updateProfile(name: String, phone: String): UserProfile? {
        val current = currentUser() ?: return null
        prefs.edit()
            .putString(KEY_NAME, name.trim().ifBlank { current.name })
            .putString(KEY_PHONE, phone.trim())
            .apply()

        return current.copy(
            name = name.trim().ifBlank { current.name },
            phone = phone.trim(),
            isLoggedIn = true
        )
    }

    fun logout() {
        firebaseAuthOrNull()?.signOut()
        prefs.edit().clear().apply()
    }

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    private fun firebaseAuthOrNull(): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAuth.getInstance()
        } catch (_: IllegalStateException) {
            null
        }
    }

    private suspend fun loginWithBackend(email: String, password: String): UserProfile? {
        return runCatching {
            val response = api.login(AuthRequest(email = email, password = password))
            val body = response.body()
            if (!response.isSuccessful || body == null) return@runCatching null
            saveBackendSession(body)
        }.getOrNull()
    }

    private suspend fun signupWithBackend(name: String, email: String, password: String): UserProfile? {
        return runCatching {
            val response = api.signup(AuthRequest(email = email, password = password, name = name))
            val body = response.body()
            if (!response.isSuccessful || body == null) return@runCatching null
            saveBackendSession(body)
        }.getOrNull()
    }

    private fun saveBackendSession(response: AuthResponse): UserProfile {
        val profile = UserProfile(
            id = response.user.id,
            name = response.user.name,
            email = response.user.email,
            isLoggedIn = true
        )
        prefs.edit()
            .putString(KEY_USER_ID, profile.id)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_TOKEN, response.token)
            .apply()
        return profile
    }

    private fun saveLocalSession(email: String, name: String, phone: String = "") {
        prefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_NAME, name)
            .putString(KEY_PHONE, phone)
            .apply()
    }

    private suspend fun Task<AuthResult>.awaitTask(): AuthResult = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWith(Result.failure(task.exception ?: IllegalStateException("Firebase authentication failed.")))
            }
        }
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_PHONE = "phone"
        const val KEY_TOKEN = "token"
    }
}

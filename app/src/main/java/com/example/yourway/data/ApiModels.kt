package com.example.yourway.data

import com.example.yourway.model.SupportMessage
import com.google.gson.annotations.SerializedName

data class SmsSyncRequest(
    val sender: String,
    val message: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val category: String? = null
)

data class DemoAddMoneyRequest(
    val amount: Double,
    val method: String,
    val provider: String? = null,
    val referenceId: String? = null,
    val cardNetwork: String? = null,
    val bank: String? = null,
    val status: String = "SUCCESS"
)

data class BuyPaintingRequest(
    val paintingId: String,
    val quantity: Int
)

data class WithdrawalRequestBody(
    val name: String,
    val bankAccount: String,
    val ifsc: String,
    val upiId: String,
    val amount: Double
)

data class AuthRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String
)

data class SupportMessageRequest(
    val body: String
)

data class SupportMessageDto(
    @SerializedName("_id")
    val mongoId: String? = null,
    val id: String? = null,
    val userId: String? = null,
    val userEmail: String? = null,
    val body: String = "",
    val fromSupport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SupportMessage {
        return SupportMessage(
            remoteId = id ?: mongoId.orEmpty(),
            body = body,
            fromSupport = fromSupport,
            createdAt = createdAt
        )
    }
}

data class SupportMessagesResponse(
    val data: List<SupportMessageDto> = emptyList()
)

data class SupportMessageResponse(
    val data: SupportMessageDto
)

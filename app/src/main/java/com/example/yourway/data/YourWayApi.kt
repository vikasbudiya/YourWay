package com.example.yourway.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface YourWayApi {
    @POST("sms")
    suspend fun syncSms(@Body request: SmsSyncRequest): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signup(@Body request: AuthRequest): Response<AuthResponse>

    @POST("api/wallet/demo-add")
    suspend fun addDemoMoney(
        @Header("Authorization") bearerToken: String?,
        @Body request: DemoAddMoneyRequest
    ): Response<Unit>

    @POST("api/investments/buy")
    suspend fun buyPainting(
        @Header("Authorization") bearerToken: String?,
        @Body request: BuyPaintingRequest
    ): Response<Unit>

    @POST("api/withdrawals")
    suspend fun requestWithdrawal(
        @Header("Authorization") bearerToken: String?,
        @Body request: WithdrawalRequestBody
    ): Response<Unit>

    @GET("api/support/messages")
    suspend fun supportMessages(
        @Header("Authorization") bearerToken: String?
    ): Response<SupportMessagesResponse>

    @POST("api/support/messages")
    suspend fun sendSupportMessage(
        @Header("Authorization") bearerToken: String?,
        @Body request: SupportMessageRequest
    ): Response<SupportMessageResponse>
}

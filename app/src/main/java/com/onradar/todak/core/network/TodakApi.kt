package com.onradar.todak.core.network

import com.onradar.todak.data.model.CheckRequest
import com.onradar.todak.data.model.CheckResponse
import com.onradar.todak.data.model.ConversationTurnResponse
import com.onradar.todak.data.model.EmergencyRequest
import com.onradar.todak.data.model.EmergencyResponse
import com.onradar.todak.data.model.EndConversationResponse
import com.onradar.todak.data.model.FcmTokenRequest
import com.onradar.todak.data.model.MessageRequest
import com.onradar.todak.data.model.RegisterRequest
import com.onradar.todak.data.model.RegisterResponse
import com.onradar.todak.data.model.StartConversationRequest
import com.onradar.todak.data.model.TodayResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** 어르신 앱이 쓰는 Core API 9개. 등록을 빼면 모두 X-Device-Token 헤더가 붙습니다. */
interface TodakApi {
    @POST("devices/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @PUT("devices/fcm-token")
    suspend fun updateFcmToken(@Body body: FcmTokenRequest)

    @GET("seniors/me/today")
    suspend fun today(): TodayResponse

    @POST("checks")
    suspend fun submitCheck(@Body body: CheckRequest): CheckResponse

    @PUT("checks/{checkId}")
    suspend fun updateCheck(@Path("checkId") checkId: Long, @Body body: CheckRequest): CheckResponse

    @POST("conversations")
    suspend fun startConversation(@Body body: StartConversationRequest): ConversationTurnResponse

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") conversationId: Long, @Body body: MessageRequest): ConversationTurnResponse

    @POST("conversations/{id}/end")
    suspend fun endConversation(@Path("id") conversationId: Long): EndConversationResponse

    @POST("emergency")
    suspend fun emergency(@Body body: EmergencyRequest): EmergencyResponse
}

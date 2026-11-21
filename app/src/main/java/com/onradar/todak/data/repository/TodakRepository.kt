package com.onradar.todak.data.repository

import com.onradar.todak.core.network.NetworkModule
import com.onradar.todak.core.network.TodakApi
import com.onradar.todak.data.model.ApiErrorBody
import com.onradar.todak.data.model.CheckRequest
import com.onradar.todak.data.model.CheckResponse
import com.onradar.todak.data.model.ConversationTurnResponse
import com.onradar.todak.data.model.EmergencyRequest
import com.onradar.todak.data.model.EmergencyResponse
import com.onradar.todak.data.model.MessageRequest
import com.onradar.todak.data.model.RegisterRequest
import com.onradar.todak.data.model.RegisterResponse
import com.onradar.todak.data.model.StartConversationRequest
import com.onradar.todak.data.model.TodayResponse
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * 앱이 쓰는 데이터 창구. 화면(ViewModel)은 이 인터페이스만 봅니다.
 * BuildConfig.USE_MOCK이 true면 FakeTodakRepository, 아니면 RemoteTodakRepository가 들어갑니다.
 */
interface TodakRepository {
    suspend fun register(registrationCode: String, fcmToken: String?): Result<RegisterResponse>
    suspend fun today(): Result<TodayResponse>
    suspend fun submitCheck(request: CheckRequest): Result<CheckResponse>
    suspend fun updateCheck(checkId: Long, request: CheckRequest): Result<CheckResponse>
    suspend fun startConversation(checkId: Long): Result<ConversationTurnResponse>
    suspend fun sendMessage(conversationId: Long, text: String): Result<ConversationTurnResponse>
    suspend fun endConversation(conversationId: Long): Result<Unit>
    suspend fun emergency(): Result<EmergencyResponse>
}

/** 서버가 돌려준 에러 (예: 409 CHECK_ALREADY_SUBMITTED). */
class ApiException(val httpCode: Int, val code: String?, override val message: String) : Exception(message)

class RemoteTodakRepository(private val api: TodakApi) : TodakRepository {

    override suspend fun register(registrationCode: String, fcmToken: String?) =
        call { api.register(RegisterRequest(registrationCode, fcmToken)) }

    override suspend fun today() = call { api.today() }

    override suspend fun submitCheck(request: CheckRequest) = call { api.submitCheck(request) }

    override suspend fun updateCheck(checkId: Long, request: CheckRequest) =
        call { api.updateCheck(checkId, request) }

    override suspend fun startConversation(checkId: Long) =
        call { api.startConversation(StartConversationRequest(checkId)) }

    override suspend fun sendMessage(conversationId: Long, text: String) =
        call { api.sendMessage(conversationId, MessageRequest(text)) }

    override suspend fun endConversation(conversationId: Long) =
        call { api.endConversation(conversationId); Unit }

    override suspend fun emergency() = call { api.emergency(EmergencyRequest()) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        val parsed = body?.let { runCatching { NetworkModule.json.decodeFromString<ApiErrorBody>(it) }.getOrNull() }
        Result.failure(ApiException(e.code(), parsed?.code, parsed?.message ?: "잠시 후 다시 해 주세요"))
    } catch (e: Exception) {
        Result.failure(ApiException(-1, null, "인터넷 연결을 확인해 주세요"))
    }
}

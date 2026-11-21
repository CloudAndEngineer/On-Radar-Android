package com.onradar.todak.data.model

import kotlinx.serialization.Serializable

/*
 * Core API 요청·응답 모델. 기준 문서: API 명세서 (2) (11/21 피드백 반영본)
 * - 필드는 camelCase, 시각은 한국 시간 ISO-8601 문자열
 * - 아침은 conditionScore만, 저녁은 mealCount만 보냅니다. (수면·복약 질문 삭제)
 */

@Serializable
enum class Session { MORNING, EVENING }

@Serializable
enum class CheckStatus { PENDING, DONE, MISSED }

@Serializable
enum class RiskLevel { NORMAL, CAUTION, DANGER }

/** conditionScore: 2 좋아요 · 1 그저 그래요 · 0 안 좋아요 */
object ConditionScore {
    const val GOOD = 2
    const val SO_SO = 1
    const val BAD = 0
}

// POST /devices/register
@Serializable
data class RegisterRequest(val registrationCode: String, val fcmToken: String? = null)

@Serializable
data class RegisterResponse(
    val seniorId: Long,
    val deviceToken: String,
    val name: String,
    val honorific: String,
)

// PUT /devices/fcm-token
@Serializable
data class FcmTokenRequest(val fcmToken: String)

// GET /seniors/me/today
@Serializable
data class TodayResponse(
    val date: String,
    val morning: SessionState,
    val evening: SessionState,
    val callContact: CallContact? = null, // 가족 기능(TODO) 전까지 null → [딸에게 전화하기] 숨김
)

@Serializable
data class SessionState(
    val status: CheckStatus,
    val checkId: Long? = null,
    val respondedAt: String? = null,
    val opensAt: String? = null,
)

@Serializable
data class CallContact(val label: String, val phone: String)

// POST /checks , PUT /checks/{checkId}
@Serializable
data class CheckRequest(
    val session: Session,
    val conditionScore: Int? = null, // 아침
    val mealCount: Int? = null,      // 저녁 (0~3)
) {
    companion object {
        fun morning(conditionScore: Int) = CheckRequest(Session.MORNING, conditionScore = conditionScore)
        fun evening(mealCount: Int) = CheckRequest(Session.EVENING, mealCount = mealCount)
    }
}

@Serializable
data class CheckResponse(
    val checkId: Long,
    val respondedAt: String,
    val riskLevel: RiskLevel,
)

// POST /conversations , /conversations/{id}/messages
@Serializable
data class StartConversationRequest(val checkId: Long)

@Serializable
data class MessageRequest(val text: String)

@Serializable
data class ConversationTurnResponse(
    val conversationId: Long? = null, // 시작 응답에만 옴
    val aiMessage: String,
    val turn: Int,
    val ended: Boolean,
)

// POST /conversations/{id}/end
@Serializable
data class EndConversationResponse(val conversationId: Long, val saved: Boolean)

// POST /emergency
@Serializable
data class EmergencyRequest(val source: String = "BUTTON")

@Serializable
data class EmergencyResponse(
    val alertId: Long,
    val level: String,
    val message: String,
)

// 에러 응답 본문: {"code": "CHECK_ALREADY_SUBMITTED", "message": "..."}
@Serializable
data class ApiErrorBody(val code: String, val message: String)

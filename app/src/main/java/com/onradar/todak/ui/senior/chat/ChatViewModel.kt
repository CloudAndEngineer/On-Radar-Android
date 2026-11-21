package com.onradar.todak.ui.senior.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onradar.todak.core.speech.TtsManager
import com.onradar.todak.data.model.ConversationTurnResponse
import com.onradar.todak.data.repository.TodakRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 체크 뒤 짧은 대화 (최대 3턴). AI가 먼저 말을 겁니다.
 * 심사 피드백: "듣고 있어요" / "답변을 준비하고 있어요"를 글과 표시로 구분해
 * 반복해서 누르거나 멈췄다고 생각하지 않게 합니다.
 *
 * 앱은 AI 답·분류·요약을 지어내지 않습니다. Core가 정상 응답으로 준 대체 문장은 그대로 읽고,
 * 앱↔Core 통신 실패는 ERROR로 따로 보여 줍니다.
 */
enum class ChatPhase {
    PREPARING,   // AI 답 기다리는 중
    AI_SPEAKING, // AI 말을 읽어 주는 중
    READY,       // [누르고 말씀하세요] 대기
    LISTENING,   // 듣고 있어요
    MIC_FAILED,  // 마이크 권한 없음·인식 실패 → 다시 말하기 / 다음에 할게요
    ERROR,       // 서버와 통신 실패 → 다시 해 보기 / 다음에 할게요
    FINISHED,
}

data class ChatMessage(val fromAi: Boolean, val text: String)

data class ChatUiState(
    val phase: ChatPhase = ChatPhase.PREPARING,
    val messages: List<ChatMessage> = emptyList(),
    val failReason: String? = null,
)

class ChatViewModel(
    private val checkId: Long,
    private val repository: TodakRepository,
    private val tts: TtsManager,
    /** 화면이 사라져도 /end 요청이 취소되지 않게 앱 수명의 scope를 씁니다. */
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private var conversationId: Long? = null
    /** 마지막으로 실패한 행동을 다시 시도하기 위해 기억합니다. */
    private var retry: (() -> Unit)? = null

    init { start() }

    private fun start() {
        setPhase(ChatPhase.PREPARING)
        // 시작 요청도 앱 scope에서: 기다리는 중에 [다음에 할게요]로 화면이 닫혀도 늦게 온 대화를 서버에서 닫을 수 있게.
        appScope.launch {
            repository.startConversation(checkId)
                .onSuccess { res ->
                    if (isFinished()) {
                        // 응답이 오기 전에 [다음에 할게요]를 눌렀다면 바로 닫습니다.
                        res.conversationId?.let(::endInBackground)
                        return@onSuccess
                    }
                    conversationId = res.conversationId
                    onAiTurn(res)
                }
                .onFailure { showError { start() } }
        }
    }

    fun onListening() = setPhase(ChatPhase.LISTENING, failReason = null)

    fun onMicUnavailable(reason: String) = setPhase(ChatPhase.MIC_FAILED, failReason = reason)

    /** 음성 인식 결과를 보냅니다. */
    fun send(text: String) {
        val id = conversationId ?: return
        if (isFinished()) return
        tts.stop()
        _state.update {
            it.copy(phase = ChatPhase.PREPARING, messages = it.messages + ChatMessage(false, text), failReason = null)
        }
        sendToServer(id, text)
    }

    private fun sendToServer(id: Long, text: String) {
        setPhase(ChatPhase.PREPARING)
        viewModelScope.launch {
            repository.sendMessage(id, text)
                .onSuccess { if (!isFinished()) onAiTurn(it) }
                .onFailure { showError { sendToServer(id, text) } }
        }
    }

    fun retryLast() = retry?.invoke()

    /** [도와주세요]로 화면을 떠날 때. 돌아오면 버튼으로 다시 말할 수 있게 READY로 둡니다. */
    fun stopSpeaking() {
        tts.stop()
        _state.update {
            if (it.phase == ChatPhase.AI_SPEAKING || it.phase == ChatPhase.LISTENING) it.copy(phase = ChatPhase.READY) else it
        }
    }

    /** [다음에 할게요] — 대화를 끝내고 분류·요약은 서버에 맡깁니다. */
    fun skip() {
        tts.stop()
        conversationId?.let(::endInBackground)
        setPhase(ChatPhase.FINISHED)
    }

    private fun endInBackground(id: Long) {
        appScope.launch {
            // 실패해도 체크는 이미 저장돼 있으므로 완료 화면은 그대로 둡니다 (대화 저장 성공이라고 표시하지 않음).
            withTimeoutOrNull(END_TIMEOUT_MS) { repository.endConversation(id) }
        }
    }

    private fun onAiTurn(res: ConversationTurnResponse) {
        _state.update {
            it.copy(phase = ChatPhase.AI_SPEAKING, messages = it.messages + ChatMessage(true, res.aiMessage))
        }
        tts.speak(res.aiMessage) {
            // ended: true면 /end를 따로 부르지 않고 완료 화면으로 갑니다. (API 명세서)
            _state.update { s ->
                if (s.phase != ChatPhase.AI_SPEAKING) s
                else s.copy(phase = if (res.ended) ChatPhase.FINISHED else ChatPhase.READY)
            }
        }
    }

    private fun showError(retryAction: () -> Unit) {
        if (isFinished()) return
        retry = retryAction
        val line = "지금은 이야기를 이어가기 어려워요. 오늘 체크는 저장됐어요."
        setPhase(ChatPhase.ERROR, failReason = line)
        tts.speak(line)
    }

    private fun isFinished() = _state.value.phase == ChatPhase.FINISHED

    private fun setPhase(phase: ChatPhase, failReason: String? = _state.value.failReason) =
        _state.update { if (it.phase == ChatPhase.FINISHED) it else it.copy(phase = phase, failReason = failReason) }

    override fun onCleared() {
        tts.stop()
    }

    private companion object {
        const val END_TIMEOUT_MS = 15_000L
    }
}

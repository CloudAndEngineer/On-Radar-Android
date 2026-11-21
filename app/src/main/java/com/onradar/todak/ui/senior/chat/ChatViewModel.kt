package com.onradar.todak.ui.senior.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onradar.todak.core.speech.TtsManager
import com.onradar.todak.data.model.ConversationTurnResponse
import com.onradar.todak.data.repository.TodakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 체크 뒤 짧은 대화 (최대 3턴). AI가 먼저 말을 겁니다.
 * 심사 피드백: "듣고 있어요" / "답변을 준비하고 있어요"를 글과 표시로 구분해
 * 반복해서 누르거나 멈췄다고 생각하지 않게 합니다.
 */
enum class ChatPhase {
    PREPARING,  // AI 답 기다리는 중
    AI_SPEAKING, // AI 말을 읽어 주는 중
    READY,      // [누르고 말씀하세요] 대기
    LISTENING,  // 듣고 있어요
    MIC_FAILED, // 마이크 권한 없음·인식 실패 → 버튼으로 답하기
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
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private var conversationId: Long? = null

    init {
        viewModelScope.launch {
            repository.startConversation(checkId)
                .onSuccess { res ->
                    conversationId = res.conversationId
                    onAiTurn(res)
                }
                .onFailure { finishWith("말씀 고마워요. 오늘도 편안한 하루 보내세요.") }
        }
    }

    fun onListening() = _state.update { it.copy(phase = ChatPhase.LISTENING, failReason = null) }

    fun onMicUnavailable(reason: String) =
        _state.update { it.copy(phase = ChatPhase.MIC_FAILED, failReason = reason) }

    /** 음성 인식 결과 또는 버튼 답을 보냅니다. */
    fun send(text: String) {
        val id = conversationId ?: return
        tts.stop()
        _state.update {
            it.copy(phase = ChatPhase.PREPARING, messages = it.messages + ChatMessage(false, text), failReason = null)
        }
        viewModelScope.launch {
            repository.sendMessage(id, text)
                .onSuccess(::onAiTurn)
                // Core가 5초 안에 대체 문장을 주지만, 네트워크 자체가 끊겼을 때를 대비
                .onFailure { finishWith("말씀 고마워요. 오늘도 편안한 하루 보내세요.") }
        }
    }

    /** [도와주세요]로 화면을 떠날 때. 돌아오면 버튼으로 다시 말할 수 있게 READY로 둡니다. */
    fun stopSpeaking() {
        tts.stop()
        _state.update {
            if (it.phase == ChatPhase.AI_SPEAKING || it.phase == ChatPhase.LISTENING) it.copy(phase = ChatPhase.READY) else it
        }
    }

    /** [다음에 할게요] — 대화를 끝내고 분류·요약을 맡깁니다. */
    fun skip() {
        tts.stop()
        val id = conversationId
        _state.update { it.copy(phase = ChatPhase.FINISHED) }
        if (id != null) viewModelScope.launch { repository.endConversation(id) }
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

    private fun finishWith(line: String) {
        _state.update { it.copy(phase = ChatPhase.AI_SPEAKING, messages = it.messages + ChatMessage(true, line)) }
        tts.speak(line) { _state.update { it.copy(phase = ChatPhase.FINISHED) } }
    }

    override fun onCleared() {
        tts.stop()
    }
}

package com.onradar.todak.ui.senior.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.onradar.todak.core.designsystem.ChoiceButton
import com.onradar.todak.core.designsystem.ChoiceTile
import com.onradar.todak.core.designsystem.PrimaryButton
import com.onradar.todak.core.designsystem.ScreenTitle
import com.onradar.todak.core.designsystem.SecondaryButton
import com.onradar.todak.core.designsystem.SessionBadge
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakScaffold
import com.onradar.todak.data.model.CheckRequest
import com.onradar.todak.data.model.ConditionScore
import com.onradar.todak.data.model.Session
import com.onradar.todak.data.repository.ApiException
import com.onradar.todak.data.repository.TodakRepository
import com.onradar.todak.ui.common.LoadingMessage
import com.onradar.todak.ui.common.appContainer
import com.onradar.todak.ui.common.todakViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 한 화면에 질문 하나 (유저 플로우, 11/21 피드백 반영).
 * 아침: ① 인사 → ② 몸 상태 / 저녁: ① 인사 → ② 밥 → 제출 후 ③ 짧은 대화로 이어집니다.
 */
enum class CheckStep { GREETING, QUESTION, SUBMITTING }

data class CheckUiState(
    val step: CheckStep = CheckStep.GREETING,
    val error: String? = null,
    val submittedCheckId: Long? = null,
)

class CheckViewModel(
    private val session: Session,
    private val repository: TodakRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckUiState())
    val state = _state.asStateFlow()

    fun next() = _state.update { it.copy(step = CheckStep.QUESTION, error = null) }
    fun back() = _state.update { it.copy(step = CheckStep.GREETING, error = null) }

    fun answer(value: Int) {
        val request = when (session) {
            Session.MORNING -> CheckRequest.morning(conditionScore = value)
            Session.EVENING -> CheckRequest.evening(mealCount = value)
        }
        _state.update { it.copy(step = CheckStep.SUBMITTING, error = null) }
        viewModelScope.launch {
            val result = repository.submitCheck(request).recoverCatching { e ->
                // 같은 날 같은 세션을 다시 보내면 409 → 오늘 checkId를 찾아 수정(PUT)합니다.
                if (e is ApiException && e.code == "CHECK_ALREADY_SUBMITTED") {
                    val today = repository.today().getOrThrow()
                    val id = (if (session == Session.MORNING) today.morning else today.evening).checkId
                        ?: throw e
                    repository.updateCheck(id, request).getOrThrow()
                } else {
                    throw e
                }
            }
            result
                .onSuccess { res -> _state.update { it.copy(submittedCheckId = res.checkId) } }
                .onFailure { e -> _state.update { it.copy(step = CheckStep.QUESTION, error = e.message) } }
        }
    }
}

@Composable
fun CheckScreen(
    session: Session,
    displayName: String,
    onSubmitted: (checkId: Long) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
) {
    val vm = todakViewModel(key = "check-$session") { CheckViewModel(session, it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val isMorning = session == Session.MORNING

    LaunchedEffect(state.submittedCheckId) {
        state.submittedCheckId?.let(onSubmitted)
    }

    // 인사와 질문을 소리로도 읽어 줍니다. 단계가 바뀔 때 한 번만, 화면을 떠나면 멈춥니다.
    val tts = appContainer().tts
    val greeting = if (isMorning) "좋은 아침이에요, $displayName!" else "좋은 저녁이에요, $displayName!"
    LaunchedEffect(state.step) {
        when (state.step) {
            CheckStep.GREETING -> tts.speak(greeting)
            CheckStep.QUESTION -> tts.speak(if (isMorning) MORNING_QUESTION else EVENING_QUESTION)
            CheckStep.SUBMITTING -> tts.stop()
        }
    }
    DisposableEffect(Unit) { onDispose { tts.stop() } }

    TodakScaffold(onHelpClick = { tts.stop(); onHelp() }) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SessionBadge(isMorning)
            when (state.step) {
                CheckStep.GREETING -> {
                    ScreenTitle(greeting.replace(", ", ",\n"))
                    Spacer(Modifier.weight(1f))
                    PrimaryButton(
                        if (isMorning) "네, 일어났어요" else "네, 안녕하세요",
                        onClick = vm::next,
                        containerColor = if (isMorning) TodakColors.Primary else TodakColors.Evening,
                    )
                    SecondaryButton("이전", onClick = onBack)
                }
                CheckStep.QUESTION -> {
                    // 선택지 영역만 스크롤되고 [이전]은 항상 아래에 보이게 합니다.
                    // (Note 8 · 글꼴 1.1배에서 '못 먹었어요'가 잘리던 문제)
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (isMorning) MorningQuestion(vm::answer) else EveningQuestion(vm::answer)
                        state.error?.let {
                            Text(it, color = TodakColors.Emergency, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (scroll.canScrollForward) {
                        Text(
                            "▼ 아래로 밀면 더 있어요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TodakColors.OnSurfaceMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SecondaryButton("이전", onClick = vm::back)
                }
                CheckStep.SUBMITTING -> LoadingMessage("저장하고 있어요")
            }
        }
    }
}

@Composable
private fun MorningQuestion(onAnswer: (Int) -> Unit) {
    ScreenTitle(MORNING_QUESTION)
    ChoiceButton("😊", "좋아요", TodakColors.Good, TodakColors.GoodContainer, { onAnswer(ConditionScore.GOOD) })
    ChoiceButton("😐", "그저 그래요", TodakColors.SoSo, TodakColors.SoSoContainer, { onAnswer(ConditionScore.SO_SO) })
    ChoiceButton("😣", "안 좋아요", TodakColors.Bad, TodakColors.BadContainer, { onAnswer(ConditionScore.BAD) })
}

@Composable
private fun EveningQuestion(onAnswer: (Int) -> Unit) {
    ScreenTitle(EVENING_QUESTION)
    // 밥그릇 개수로 횟수를 표시합니다. 네 개가 한 화면에 들어오도록 2×2로 놓습니다.
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceTile("🍚", "한 번", TodakColors.Evening, TodakColors.EveningContainer, { onAnswer(1) }, Modifier.weight(1f))
        ChoiceTile("🍚🍚", "두 번", TodakColors.Evening, TodakColors.EveningContainer, { onAnswer(2) }, Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceTile("🍚🍚🍚", "세 번", TodakColors.Evening, TodakColors.EveningContainer, { onAnswer(3) }, Modifier.weight(1f))
        ChoiceTile("🥣", "못 먹었어요", TodakColors.OnSurfaceMuted, TodakColors.Surface, { onAnswer(0) }, Modifier.weight(1f))
    }
}

private const val MORNING_QUESTION = "오늘 몸은 어떠세요?"
private const val EVENING_QUESTION = "오늘 밥은 몇 번 드셨어요?"

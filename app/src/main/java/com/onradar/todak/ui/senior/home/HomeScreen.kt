package com.onradar.todak.ui.senior.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.onradar.todak.BuildConfig
import com.onradar.todak.core.designsystem.PrimaryButton
import com.onradar.todak.core.designsystem.ScreenTitle
import com.onradar.todak.core.designsystem.SecondaryButton
import com.onradar.todak.core.designsystem.SessionBadge
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakScaffold
import com.onradar.todak.data.model.CheckStatus
import com.onradar.todak.data.model.Session
import com.onradar.todak.data.model.TodayResponse
import com.onradar.todak.data.repository.TodakRepository
import com.onradar.todak.ui.common.LoadingMessage
import com.onradar.todak.ui.common.todakViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

/** 홈에서 지금 보여줄 "할 일 하나". */
sealed interface HomeAction {
    data class DoCheck(val session: Session) : HomeAction
    /** 체크를 마친 뒤: [이야기 나누기] + 다음 체크 안내 */
    data class Talk(val checkId: Long, val session: Session, val nextGuide: String) : HomeAction
}

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val action: HomeAction) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: TodakRepository) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state = _state.asStateFlow()

    fun load() {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            repository.today()
                .onSuccess { _state.value = HomeUiState.Ready(decide(it, LocalTime.now())) }
                .onFailure { _state.value = HomeUiState.Error(it.message ?: "잠시 후 다시 해 주세요") }
        }
    }

    /** 저녁 체크는 18시부터 엽니다 (today.evening.opensAt). */
    private fun decide(today: TodayResponse, now: LocalTime): HomeAction {
        val eveningOpen = now >= EVENING_OPENS
        val morning = today.morning
        val evening = today.evening
        return when {
            eveningOpen && evening.status != CheckStatus.DONE -> HomeAction.DoCheck(Session.EVENING)
            evening.status == CheckStatus.DONE && evening.checkId != null ->
                HomeAction.Talk(evening.checkId, Session.EVENING, "내일 아침 8시에 다시 알려 드릴게요")
            morning.status != CheckStatus.DONE -> HomeAction.DoCheck(Session.MORNING)
            else -> HomeAction.Talk(morning.checkId ?: 0, Session.MORNING, "저녁 6시에 다시 알려 드릴게요")
        }
    }

    private companion object {
        val EVENING_OPENS: LocalTime = LocalTime.of(18, 0)
    }
}

@Composable
fun HomeScreen(
    displayName: String,
    onStartCheck: (Session) -> Unit,
    onTalk: (checkId: Long, session: Session) -> Unit,
    onHelp: () -> Unit,
) {
    val vm = todakViewModel { HomeViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()

    // 홈으로 돌아올 때마다 오늘 상태를 새로 불러옵니다.
    LaunchedEffect(Unit) { vm.load() }

    TodakScaffold(onHelpClick = onHelp) {
        when (val s = state) {
            HomeUiState.Loading -> LoadingMessage("오늘 할 일을 불러오고 있어요")
            is HomeUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ScreenTitle("불러오지 못했어요", subtitle = s.message)
                PrimaryButton("다시 해 보기", onClick = vm::load)
            }
            is HomeUiState.Ready -> HomeContent(displayName, s.action, onStartCheck, onTalk)
        }
    }
}

@Composable
private fun HomeContent(
    displayName: String,
    action: HomeAction,
    onStartCheck: (Session) -> Unit,
    onTalk: (Long, Session) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("$displayName,\n안녕하세요", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.weight(1f))

        when (action) {
            is HomeAction.DoCheck -> {
                val isMorning = action.session == Session.MORNING
                SessionBadge(isMorning)
                Text(
                    if (isMorning) "오늘 아침 몸 상태를 알려 주세요" else "오늘 식사를 알려 주세요",
                    style = MaterialTheme.typography.headlineMedium,
                )
                PrimaryButton("오늘 체크하기", onClick = { onStartCheck(action.session) })
            }
            is HomeAction.Talk -> {
                Text(
                    "✅ 오늘 체크를 마쳤어요\n${action.nextGuide}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TodakColors.Done,
                )
                PrimaryButton("이야기 나누기", onClick = { onTalk(action.checkId, action.session) })
                // 개발용: 낮에도 저녁 체크 흐름을 볼 수 있게 (가짜 데이터 모드에서만)
                if (BuildConfig.DEBUG && BuildConfig.USE_MOCK && action.session == Session.MORNING) {
                    SecondaryButton("[개발용] 저녁 체크 미리 보기", onClick = { onStartCheck(Session.EVENING) })
                }
            }
        }
        // callContact(가족 전화)는 가족 기능(TODO) 전까지 항상 null이라 [딸에게 전화하기]를 그리지 않습니다.
        Spacer(Modifier.height(8.dp))
        Text(
            "급할 땐 아래 빨간 버튼을 누르세요",
            style = MaterialTheme.typography.bodyMedium,
            color = TodakColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

package com.onradar.todak.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.onradar.todak.core.designsystem.PrimaryButton
import com.onradar.todak.core.designsystem.ScreenTitle
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakScaffold
import com.onradar.todak.core.session.SessionStore
import com.onradar.todak.data.repository.TodakRepository
import com.onradar.todak.ui.common.todakViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 최초 1회 기기 등록. 복지사가 설치를 도우며 등록 코드를 넣고,
 * 대화 요약이 복지사에게 전달된다는 점을 설명한 뒤 동의를 받습니다. (심사 피드백 4·5)
 */
data class OnboardingUiState(
    val code: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val registered: Boolean = false,
)

class OnboardingViewModel(
    private val repository: TodakRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state = _state.asStateFlow()

    fun onCodeChange(code: String) = _state.update { it.copy(code = code.uppercase().trim(), error = null) }

    fun register() {
        val code = _state.value.code
        if (code.isBlank()) {
            _state.update { it.copy(error = "등록 코드를 넣어 주세요") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // TODO(FCM): google-services.json을 넣은 뒤 FirebaseMessaging 토큰을 함께 보냅니다.
            repository.register(code, fcmToken = null)
                .onSuccess { result ->
                    sessionStore.save(result)
                    _state.update { it.copy(loading = false, registered = true) }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }
}

@Composable
fun OnboardingScreen(onRegistered: () -> Unit) {
    val vm = todakViewModel { OnboardingViewModel(it.repository, it.sessionStore) }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.registered) {
        if (state.registered) onRegistered()
    }

    // 등록 전에는 알릴 대상이 없으므로 [도와주세요] 바를 숨깁니다.
    TodakScaffold(onHelpClick = {}, showHelpBar = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ScreenTitle("토닥 시작하기", subtitle = "복지사 선생님이 함께 설정해 드려요")

            Text(
                "• 하루 두 번, 아침·저녁에 버튼 하나만 누르시면 돼요.\n" +
                    "• 답과 짧은 대화 요약은 담당 복지사 선생님께 전달돼요.\n" +
                    "• 저는 AI예요. 병원·약 같은 의료 판단은 하지 않아요.",
                style = MaterialTheme.typography.bodyLarge,
            )

            OutlinedTextField(
                value = state.code,
                onValueChange = vm::onCodeChange,
                label = { Text("등록 코드 (예: KSJ-0001)") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { Text(it, color = TodakColors.Emergency, style = MaterialTheme.typography.bodyLarge) }

            PrimaryButton(
                text = if (state.loading) "등록하는 중이에요…" else "설명 듣고 동의했어요 · 등록하기",
                onClick = vm::register,
                enabled = !state.loading,
            )
        }
    }
}

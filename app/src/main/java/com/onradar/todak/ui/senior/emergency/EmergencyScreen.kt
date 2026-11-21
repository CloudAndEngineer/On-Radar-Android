package com.onradar.todak.ui.senior.emergency

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.onradar.todak.core.designsystem.PrimaryButton
import com.onradar.todak.core.designsystem.ScreenTitle
import com.onradar.todak.core.designsystem.SecondaryButton
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakScaffold
import com.onradar.todak.core.speech.TtsManager
import com.onradar.todak.data.repository.TodakRepository
import com.onradar.todak.ui.common.LoadingMessage
import com.onradar.todak.ui.common.todakViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [도와주세요]: POST /emergency로 복지사에게 즉시 알리고, 전화 앱에 119를 띄웁니다 (ACTION_DIAL).
 * 어르신이 직접 통화 버튼을 누릅니다. 시연에서는 다이얼 화면까지만.
 * 심사 피드백: 누구에게 연결되는지, 전달됐는지, 확인을 기다리는 중인지 쉽게 보이게.
 * [잘못 눌렀어요]는 두지 않고, 복지사가 웹에서 확인 후 해제합니다.
 */
sealed interface EmergencyUiState {
    data object Sending : EmergencyUiState
    data class Sent(val message: String) : EmergencyUiState
    data class Failed(val message: String) : EmergencyUiState
}

class EmergencyViewModel(
    private val repository: TodakRepository,
    private val tts: TtsManager,
) : ViewModel() {
    private val _state = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Sending)
    val state = _state.asStateFlow()

    init { send() }

    fun send() {
        _state.value = EmergencyUiState.Sending
        viewModelScope.launch {
            repository.emergency()
                .onSuccess {
                    _state.value = EmergencyUiState.Sent(it.message)
                    tts.speak(it.message)
                }
                .onFailure {
                    val msg = "복지사 선생님께 알리지 못했어요. 119로 바로 전화하세요."
                    _state.value = EmergencyUiState.Failed(msg)
                    tts.speak(msg)
                }
        }
    }
}

@Composable
fun EmergencyScreen(onBack: () -> Unit) {
    val vm = todakViewModel { EmergencyViewModel(it.repository, it.tts) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 알림 결과가 나오면(성공·실패 모두) 잠깐 안내를 보여 준 뒤 119 다이얼 화면을 띄웁니다.
    val settled = state !is EmergencyUiState.Sending
    LaunchedEffect(settled) {
        if (settled) {
            delay(2500)
            openDialer(context)
        }
    }

    // 이 화면 자체가 긴급 화면이라 하단 바는 숨깁니다.
    TodakScaffold(onHelpClick = {}, showHelpBar = false) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (val s = state) {
                EmergencyUiState.Sending -> LoadingMessage("복지사 선생님께\n알리고 있어요")
                is EmergencyUiState.Sent -> {
                    ScreenTitle(s.message)
                    StatusCard(
                        lines = listOf(
                            "📨 알린 곳: 담당 복지사 선생님",
                            "✅ 전달됐어요",
                            "⏳ 선생님 확인을 기다리고 있어요",
                        ),
                    )
                }
                is EmergencyUiState.Failed -> {
                    ScreenTitle(s.message)
                    SecondaryButton("다시 알리기", onClick = vm::send)
                }
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton("📞 119에 전화하기", onClick = { openDialer(context) }, containerColor = TodakColors.Emergency)
            SecondaryButton("돌아가기", onClick = onBack)
        }
    }
}

@Composable
private fun StatusCard(lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TodakColors.EmergencyContainer, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lines.forEach { Text(it, style = MaterialTheme.typography.titleLarge) }
    }
}

/** 통화 권한 없이 다이얼 화면만 엽니다. 실제 통화는 어르신이 누릅니다. */
private fun openDialer(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:119".toUri()))
    } catch (_: ActivityNotFoundException) {
        // 전화 앱이 없는 기기 (태블릿 등)
    }
}

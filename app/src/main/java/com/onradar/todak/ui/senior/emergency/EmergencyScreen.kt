package com.onradar.todak.ui.senior.emergency

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.onradar.todak.BuildConfig
import com.onradar.todak.core.designsystem.PrimaryButton
import com.onradar.todak.core.designsystem.SecondaryButton
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakScaffold
import com.onradar.todak.core.speech.TtsManager
import com.onradar.todak.data.repository.TodakRepository
import com.onradar.todak.ui.common.todakViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [도와주세요]: POST /emergency로 복지사에게 알리고, 전화 앱에 119를 띄웁니다 (ACTION_DIAL, 자동 통화 없음).
 * - 119 다이얼은 복지사 알림의 성공·실패·대기와 상관없이 항상 바로 누를 수 있습니다.
 * - 복지사의 확인·출동 여부는 API에 없으므로 앱이 지어내지 않습니다.
 * - [잘못 눌렀어요]는 두지 않고, 복지사가 웹에서 확인 후 해제합니다.
 */
sealed interface EmergencyUiState {
    data object Sending : EmergencyUiState
    /** 서버가 접수했다고 응답함. message는 서버 문구 그대로. */
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

    override fun onCleared() {
        tts.stop()
    }
}

@Composable
fun EmergencyScreen(onBack: () -> Unit) {
    val vm = todakViewModel { EmergencyViewModel(it.repository, it.tts) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 복지사 알림 응답을 기다리지 않고, 화면을 본 직후 119 다이얼 화면을 엽니다.
    LaunchedEffect(Unit) {
        delay(AUTO_DIAL_DELAY_MS)
        openDialer(context)
    }

    // 이 화면 자체가 긴급 화면이라 하단 바는 숨깁니다.
    TodakScaffold(onHelpClick = {}, showHelpBar = false) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // 119 버튼은 맨 위, 상태와 분리해 항상 같은 자리에 둡니다.
            PrimaryButton("📞 119에 전화하기", onClick = { openDialer(context) }, containerColor = TodakColors.Emergency)

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (val s = state) {
                    EmergencyUiState.Sending -> StatusCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(Modifier.size(36.dp), color = TodakColors.Emergency, strokeWidth = 4.dp)
                            Text("복지사 선생님께 알리고 있어요", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    is EmergencyUiState.Sent -> StatusCard {
                        Text(s.message, style = MaterialTheme.typography.headlineMedium)
                        if (!BuildConfig.USE_MOCK) {
                            Text("📨 알린 곳: 담당 복지사 선생님", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    is EmergencyUiState.Failed -> StatusCard {
                        Text(s.message, style = MaterialTheme.typography.headlineMedium)
                        SecondaryButton("복지사 선생님께 다시 알리기", onClick = vm::send)
                    }
                }
            }
            SecondaryButton("돌아가기", onClick = onBack)
        }
    }
}

@Composable
private fun StatusCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TodakColors.EmergencyContainer, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

private const val AUTO_DIAL_DELAY_MS = 1_500L

/** 통화 권한 없이 다이얼 화면만 엽니다. 실제 통화는 어르신이 누릅니다. */
private fun openDialer(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:119".toUri()))
    } catch (_: ActivityNotFoundException) {
        // 전화 앱이 없는 기기 (태블릿 등)
    }
}

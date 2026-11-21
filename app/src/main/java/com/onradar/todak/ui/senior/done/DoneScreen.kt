package com.onradar.todak.ui.senior.done

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onradar.todak.core.designsystem.PrimaryButton
import com.onradar.todak.core.designsystem.SecondaryButton
import com.onradar.todak.core.designsystem.SessionBadge
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakScaffold
import com.onradar.todak.data.model.Session
import com.onradar.todak.ui.common.appContainer

/** ④ 완료. 다음 체크 일정을 알려 줍니다. (심사 피드백: 완료 후 다음 확인 시간 안내) */
@Composable
fun DoneScreen(
    session: Session,
    onHome: () -> Unit,
    onRedo: () -> Unit,
    onHelp: () -> Unit,
) {
    val isMorning = session == Session.MORNING
    // FCM 알림이 연결되기 전까지는 "알려 드릴게요"라고 약속하지 않고 일정만 안내합니다.
    // 정확한 시각은 홈에서 서버 opensAt으로 보여 줍니다.
    val nextGuide = if (isMorning) "다음 체크는 저녁이에요" else "다음 체크는 내일 아침이에요"
    val tts = appContainer().tts

    LaunchedEffect(Unit) { tts.speak("오늘 체크 끝! $nextGuide") }

    TodakScaffold(onHelpClick = onHelp) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SessionBadge(isMorning)
            Text("✅", fontSize = 80.sp)
            Text(
                if (isMorning) "오늘 아침 체크 끝!" else "오늘 저녁 체크 끝!",
                style = MaterialTheme.typography.displayLarge,
                color = TodakColors.Done,
            )
            Text(nextGuide, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            PrimaryButton("처음으로", onClick = onHome)
            // 잘못 눌렀으면 다시 할 수 있습니다 (PUT /checks/{checkId}).
            SecondaryButton("다시 하기", onClick = onRedo)
        }
    }
}

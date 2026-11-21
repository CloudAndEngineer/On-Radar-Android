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
import com.onradar.todak.core.designsystem.PrimaryButton
import com.onradar.todak.core.designsystem.SecondaryButton
import com.onradar.todak.core.designsystem.SessionBadge
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakScaffold
import com.onradar.todak.data.model.ConditionScore
import com.onradar.todak.data.model.Session
import com.onradar.todak.ui.common.appContainer

/**
 * ④ 완료. 저장된 답과 다음 체크 일정을 보여 줍니다. (심사 피드백: 완료 후 다음 확인 시간 안내)
 * answer는 서버 저장에 성공한 답만 넘어옵니다. 모르면(홈에서 대화로 들어온 경우) 줄을 숨깁니다.
 */
@Composable
fun DoneScreen(
    session: Session,
    answer: Int?,
    onHome: () -> Unit,
    onRedo: () -> Unit,
    onHelp: () -> Unit,
) {
    val isMorning = session == Session.MORNING
    // FCM 알림이 연결되기 전까지는 "알려 드릴게요"라고 약속하지 않고 일정만 안내합니다.
    // 정확한 시각은 홈에서 서버 opensAt으로 보여 줍니다.
    val nextGuide = if (isMorning) "다음 체크는 저녁이에요" else "다음 체크는 내일 아침이에요"
    val answerLine = answer?.let { answerText(session, it) }
    val tts = appContainer().tts

    LaunchedEffect(Unit) {
        tts.speak(listOfNotNull("오늘 체크 끝!", answerLine, nextGuide).joinToString(" "))
    }

    TodakScaffold(onHelpClick = onHelp) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SessionBadge(isMorning)
            Text(
                if (isMorning) "✅ 오늘 아침 체크 끝!" else "✅ 오늘 저녁 체크 끝!",
                style = MaterialTheme.typography.headlineLarge,
                color = TodakColors.Done,
            )
            answerLine?.let { Text(it, style = MaterialTheme.typography.headlineMedium) }
            Text(nextGuide, style = MaterialTheme.typography.bodyLarge, color = TodakColors.OnSurfaceMuted)
            Spacer(Modifier.weight(1f))
            PrimaryButton("홈으로", onClick = onHome)
            // 잘못 눌렀으면 답만 바꿀 수 있습니다 (PUT /checks/{checkId}).
            SecondaryButton("답변 바꾸기", onClick = onRedo)
        }
    }
}

private fun answerText(session: Session, answer: Int): String = when (session) {
    Session.MORNING -> "몸 상태: " + when (answer) {
        ConditionScore.GOOD -> "좋아요"
        ConditionScore.SO_SO -> "그저 그래요"
        else -> "안 좋아요"
    }
    Session.EVENING -> "밥: " + when (answer) {
        0 -> "못 먹었어요"
        1 -> "한 번"
        2 -> "두 번"
        else -> "세 번"
    }
}

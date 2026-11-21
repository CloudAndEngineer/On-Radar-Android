package com.onradar.todak.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * 토닥 컬러 시스템.
 * 심사 피드백: 빨강은 [도와주세요](응급)에만 쓰고, 주요 버튼은 다른 색으로 용도를 구분합니다.
 */
object TodakColors {
    val Background = Color(0xFFFBF5EE)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF1F1F1F)
    val OnSurfaceMuted = Color(0xFF5C5C5C)
    val Outline = Color(0xFFD9D2C9)

    // 주요 행동 ([오늘 체크하기], [다음] 등)
    val Primary = Color(0xFF2F6F5E)
    val OnPrimary = Color(0xFFFFFFFF)

    // 응급 전용 ([도와주세요])
    val Emergency = Color(0xFFC62828)
    val OnEmergency = Color(0xFFFFFFFF)
    val EmergencyContainer = Color(0xFFFDECEA)

    // 아침·저녁 구분 (심사 피드백: 시각적으로 구분)
    val Morning = Color(0xFFF2A93B)
    val MorningContainer = Color(0xFFFFF3DC)
    /** 아침 배지 글자색. Morning(주황)은 연노랑 배경 위 대비 1.8:1이라 글자에는 진한 갈색(약 8:1)을 씁니다. */
    val OnMorningContainer = Color(0xFF6B4000)
    val Evening = Color(0xFF3F4C8A)
    val EveningContainer = Color(0xFFE6E9F7)

    // 선택지: 색만으로 구분하지 않고 표정·글자를 함께 씁니다.
    val Good = Color(0xFF2E7D32)
    val GoodContainer = Color(0xFFE3F2E4)
    val SoSo = Color(0xFF8D6E00)
    val SoSoContainer = Color(0xFFFFF6CC)
    val Bad = Color(0xFF455A9C)
    val BadContainer = Color(0xFFE5EAF7)

    val Done = Color(0xFF2E7D32)
    val DoneContainer = Color(0xFFE3F2E4)
}

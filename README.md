# 토닥 · 어르신 앱 (On-Radar Android)

독거 어르신 돌봄 안전망 **토닥**의 어르신용 Android 앱입니다. 어르신은 하루 두 번 버튼 하나만 누르고, AI 말동무와 짧게 이야기합니다.

## 화면 흐름 (11/21 피드백 반영 기획)

```
등록(복지사가 설치·동의 안내) → 홈 → ① 인사 → ② 몸 상태(아침) / 밥(저녁) → ③ 짧은 대화 → ④ 완료
모든 화면 하단 [도와주세요] → 복지사 즉시 알림(POST /emergency) → 119 다이얼 화면
```

- 한 화면에 질문 하나, 본문 26sp 이상, 버튼 72dp 이상
- 선택지는 글자 + 표정(이모지) + 색으로 표시
- 빨강은 [도와주세요]에만 사용
- 대화 중 "답변을 준비하고 있어요" / "듣고 있어요"를 글과 표시로 구분하고, 마이크를 못 쓰면 버튼으로 답할 수 있음

## 개발 환경

| 항목 | 값 |
|---|---|
| 시연 기기 | Galaxy Note 8 (Android 9, API 28) |
| minSdk / targetSdk / compileSdk | 28 / 36 / 37 |
| AGP / Gradle / Kotlin | 9.5.2 / 9.9.2 / 2.4.21 |
| UI | Jetpack Compose (BOM 2026.09.00), Navigation Compose |
| 통신 | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| 음성 | Android SpeechRecognizer(STT) + TextToSpeech |

SDK Manager에서 **Android 17 (API 37)** 플랫폼이 필요합니다. 폰은 minSdk 28 덕분에 Android 9에서 그대로 실행됩니다.

## 실행

Android Studio에서 폴더를 열고 ▶ 실행, 또는:

```
./gradlew installDebug
```

`app/build.gradle.kts`의 `USE_MOCK`이 `true`면 백엔드 없이 앱 안의 가짜 데이터(API 명세서 JSON 예시)로 동작합니다. Core API가 배포되면 `CORE_BASE_URL`을 바꾸고 `USE_MOCK`을 `false`로 둡니다.

## 구조

```
com.onradar.todak
├── core/
│   ├── designsystem/   # 색·글씨 크기, 큰 버튼, 하단 [도와주세요] 바
│   ├── network/        # Retrofit API 9개, X-Device-Token 헤더
│   ├── session/        # 기기 등록 토큰·이름 저장
│   └── speech/         # TTS(한국어 없으면 Google TTS로 대체), STT
├── data/
│   ├── model/          # API 명세서 (2) 기준 요청·응답 모델
│   └── repository/     # Remote / Fake 저장소
└── ui/
    ├── onboarding/     # 기기 등록 + 요약 전달 동의 안내
    ├── navigation/
    └── senior/         # home, check, chat, done, emergency
```

## 아직 안 한 것 (TODO)

- FCM 푸시 (아침 8시·저녁 6시 알람, 정오 재알림) — `google-services.json` 필요
- 긴급 호출 뒤 "복지사 확인함" 상태 받기 — 백엔드와 방법 합의 필요
- 가족 기능 ([딸에게 전화하기], 보호자 화면) — 본선 범위 밖

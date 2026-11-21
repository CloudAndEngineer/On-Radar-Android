package com.onradar.todak.core.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * 폰의 TextToSpeech로 AI 말과 안내 문구를 읽어 줍니다.
 * 앱 전체에서 하나만 만들어 씁니다 (엔진 초기화가 느리기 때문).
 * 한국어 음성을 못 쓰는 폰에서도 화면은 멈추지 않게, 읽기를 건너뛰고 onDone을 바로 부릅니다.
 */
class TtsManager(context: Context) {

    private val appContext = context.applicationContext
    private var triedGoogleEngine = false

    private var initialized = false
    private var ready = false
    private var pending: Pair<String, (() -> Unit)?>? = null
    private val callbacks = mutableMapOf<String, () -> Unit>()

    // tts보다 먼저 만들어야 합니다 (create()에서 바로 씀).
    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) {
            utteranceId?.let { callbacks.remove(it)?.invoke() }
        }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            utteranceId?.let { callbacks.remove(it)?.invoke() }
        }
    }

    private var tts: TextToSpeech = create(engine = null)

    private fun create(engine: String?): TextToSpeech =
        TextToSpeech(appContext, { status -> onInit(status) }, engine).also { it.setOnUtteranceProgressListener(progress) }

    private fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 삼성 TTS는 "ko"보다 "ko-KR"을 받는 경우가 있어 둘 다 시도합니다.
            ready = listOf(Locale.KOREA, Locale.KOREAN).any { locale ->
                val result = tts.setLanguage(locale)
                result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            }
            tts.setSpeechRate(0.9f) // 어르신이 듣기 편하게 조금 천천히
            if (!ready && !triedGoogleEngine) {
                // 기본 엔진(예: 삼성 TTS)에 한국어 음성이 없으면 Google TTS로 한 번 더 시도합니다.
                triedGoogleEngine = true
                Log.w(TAG, "기본 TTS에 한국어가 없어 Google TTS로 바꿔 봅니다")
                tts.shutdown()
                tts = create(GOOGLE_TTS)
                return
            }
            if (!ready) Log.w(TAG, "한국어 TTS 음성이 없어요. 폰 설정에서 한국어 음성 데이터를 받아 주세요.")
        } else {
            Log.w(TAG, "TTS 초기화 실패: $status")
        }
        initialized = true
        pending?.let { (text, onDone) -> speak(text, onDone) }
        pending = null
    }

    /** 읽기가 끝나면 onDone을 부릅니다 (백그라운드 스레드에서 불릴 수 있음). TTS를 못 쓰면 바로 부릅니다. */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!initialized) {
            // 초기화 전에 온 마지막 요청만 기억했다가 읽습니다. 밀려난 요청은 바로 끝난 것으로 처리.
            pending?.second?.invoke()
            pending = text to onDone
            return
        }
        if (!ready) {
            onDone?.invoke()
            return
        }
        val id = UUID.randomUUID().toString()
        onDone?.let { callbacks[id] = it }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        tts.stop()
        // 멈춘 읽기의 onDone은 부르지 않습니다 (화면을 떠나는 경우라).
        callbacks.clear()
    }

    private companion object {
        const val TAG = "TtsManager"
        const val GOOGLE_TTS = "com.google.android.tts"
    }
}

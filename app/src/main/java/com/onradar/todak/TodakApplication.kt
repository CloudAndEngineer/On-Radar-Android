package com.onradar.todak

import android.app.Application
import com.onradar.todak.core.network.NetworkModule
import com.onradar.todak.core.session.SessionStore
import com.onradar.todak.core.speech.TtsManager
import com.onradar.todak.data.repository.FakeTodakRepository
import com.onradar.todak.data.repository.RemoteTodakRepository
import com.onradar.todak.data.repository.TodakRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/**
 * 앱 전역 객체를 한곳에서 만듭니다. (해커톤 범위라 DI 라이브러리 없이 수동 주입)
 */
class TodakApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    /** 화면이 사라져도 끝까지 보내야 하는 요청용 (예: 대화 종료 /end). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 시연 모드와 실서버 모드는 저장 영역을 나눠, 가짜 토큰이 실서버로 가지 않게 합니다.
    val sessionStore = SessionStore(app, mock = BuildConfig.USE_MOCK)
    val tts = TtsManager(app)

    val repository: TodakRepository =
        if (BuildConfig.USE_MOCK) FakeTodakRepository()
        else RemoteTodakRepository(NetworkModule.createApi(sessionStore))
}

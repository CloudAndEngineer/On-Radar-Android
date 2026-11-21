package com.onradar.todak

import android.app.Application
import com.onradar.todak.core.network.NetworkModule
import com.onradar.todak.core.session.SessionStore
import com.onradar.todak.core.speech.TtsManager
import com.onradar.todak.data.repository.FakeTodakRepository
import com.onradar.todak.data.repository.RemoteTodakRepository
import com.onradar.todak.data.repository.TodakRepository

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
    val sessionStore = SessionStore(app)
    val tts = TtsManager(app)

    val repository: TodakRepository =
        if (BuildConfig.USE_MOCK) FakeTodakRepository()
        else RemoteTodakRepository(NetworkModule.createApi(sessionStore))
}

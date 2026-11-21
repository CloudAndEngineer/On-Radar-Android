package com.onradar.todak.core.session

import android.content.Context
import androidx.core.content.edit
import com.onradar.todak.data.model.RegisterResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 기기 등록 결과(토큰, 어르신 이름·호칭)를 기기에 저장합니다.
 * 시연(mock) 모드와 실서버 모드는 저장 파일을 따로 써서, 시연용 가짜 토큰이 실서버로 가지 않게 합니다.
 */
class SessionStore(context: Context, mock: Boolean) {
    private val prefs = context.getSharedPreferences(
        if (mock) "todak_session_mock" else "todak_session",
        Context.MODE_PRIVATE,
    )

    private val _expired = MutableStateFlow(false)
    /** 서버가 토큰을 거부(401)하면 true. 화면은 등록 화면으로 돌아갑니다. */
    val expired = _expired.asStateFlow()

    val deviceToken: String? get() = prefs.getString(KEY_TOKEN, null)
    val name: String? get() = prefs.getString(KEY_NAME, null)
    val honorific: String? get() = prefs.getString(KEY_HONORIFIC, null)

    val isRegistered: Boolean get() = deviceToken != null

    /** 화면에서 부르는 이름. 예: "김순자 할머니" */
    val displayName: String
        get() = listOfNotNull(name, honorific).joinToString(" ").ifBlank { "어르신" }

    fun save(result: RegisterResponse) {
        prefs.edit {
            putString(KEY_TOKEN, result.deviceToken)
            putString(KEY_NAME, result.name)
            putString(KEY_HONORIFIC, result.honorific)
        }
        _expired.value = false
    }

    /** 토큰이 무효가 됐을 때: 저장 내용을 지우고 재등록이 필요하다고 알립니다. */
    fun invalidate() {
        prefs.edit { clear() }
        _expired.value = true
    }

    private companion object {
        const val KEY_TOKEN = "device_token"
        const val KEY_NAME = "name"
        const val KEY_HONORIFIC = "honorific"
    }
}

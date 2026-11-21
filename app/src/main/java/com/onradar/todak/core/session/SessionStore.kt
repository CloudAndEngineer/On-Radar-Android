package com.onradar.todak.core.session

import android.content.Context
import androidx.core.content.edit
import com.onradar.todak.data.model.RegisterResponse

/** 기기 등록 결과(토큰, 어르신 이름·호칭)를 기기에 저장합니다. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("todak_session", Context.MODE_PRIVATE)

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
    }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val KEY_TOKEN = "device_token"
        const val KEY_NAME = "name"
        const val KEY_HONORIFIC = "honorific"
    }
}

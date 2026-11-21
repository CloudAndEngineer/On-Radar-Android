package com.onradar.todak.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onradar.todak.AppContainer
import com.onradar.todak.TodakApplication

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as TodakApplication).container

/** AppContainer에서 의존성을 꺼내 ViewModel을 만듭니다. */
@Composable
inline fun <reified VM : ViewModel> todakViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}

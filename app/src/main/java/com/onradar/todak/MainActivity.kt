package com.onradar.todak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.onradar.todak.core.designsystem.TodakColors
import com.onradar.todak.core.designsystem.TodakTheme
import com.onradar.todak.ui.navigation.TodakNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodakTheme {
                Box(Modifier.fillMaxSize().background(TodakColors.Background)) {
                    TodakNavHost()
                }
            }
        }
    }
}

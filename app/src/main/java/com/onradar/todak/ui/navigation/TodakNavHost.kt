package com.onradar.todak.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.onradar.todak.data.model.Session
import com.onradar.todak.ui.common.appContainer
import com.onradar.todak.ui.onboarding.OnboardingScreen
import com.onradar.todak.ui.senior.chat.ChatScreen
import com.onradar.todak.ui.senior.check.CheckScreen
import com.onradar.todak.ui.senior.done.DoneScreen
import com.onradar.todak.ui.senior.emergency.EmergencyScreen
import com.onradar.todak.ui.senior.home.HomeScreen

/**
 * 화면 흐름 (유저 플로우, 11/21):
 * 등록 → 홈 → ① 인사 → ② 몸 상태(아침)/밥(저녁) → ③ 짧은 대화 → ④ 완료
 * 모든 화면 하단 [도와주세요] → 복지사 즉시 알림 → 119 전화 화면
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CHECK = "check/{session}"
    const val CHAT = "chat/{session}/{checkId}"
    const val DONE = "done/{session}"
    const val EMERGENCY = "emergency"

    fun check(session: Session) = "check/${session.name}"
    fun chat(session: Session, checkId: Long) = "chat/${session.name}/$checkId"
    fun done(session: Session) = "done/${session.name}"
}

@Composable
fun TodakNavHost(navController: NavHostController = rememberNavController()) {
    val sessionStore = appContainer().sessionStore
    val start = if (sessionStore.isRegistered) Routes.HOME else Routes.ONBOARDING
    val goHelp = { navController.navigate(Routes.EMERGENCY) }
    val goHome = {
        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onRegistered = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }

        composable(Routes.HOME) {
            HomeScreen(
                displayName = sessionStore.displayName,
                onStartCheck = { navController.navigate(Routes.check(it)) },
                onTalk = { checkId, session -> navController.navigate(Routes.chat(session, checkId)) },
                onHelp = goHelp,
            )
        }

        composable(Routes.CHECK, arguments = listOf(navArgument("session") { type = NavType.StringType })) { entry ->
            val session = Session.valueOf(entry.arguments?.getString("session") ?: Session.MORNING.name)
            CheckScreen(
                session = session,
                displayName = sessionStore.displayName,
                onSubmitted = { checkId ->
                    navController.navigate(Routes.chat(session, checkId)) {
                        popUpTo(Routes.HOME) // 대화에서 뒤로 가면 체크가 아니라 홈으로
                    }
                },
                onBack = { navController.popBackStack() },
                onHelp = goHelp,
            )
        }

        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("session") { type = NavType.StringType },
                navArgument("checkId") { type = NavType.LongType },
            ),
        ) { entry ->
            val session = Session.valueOf(entry.arguments?.getString("session") ?: Session.MORNING.name)
            val checkId = entry.arguments?.getLong("checkId") ?: 0L
            ChatScreen(
                checkId = checkId,
                onFinished = {
                    navController.navigate(Routes.done(session)) { popUpTo(Routes.HOME) }
                },
                onHelp = goHelp,
            )
        }

        composable(Routes.DONE, arguments = listOf(navArgument("session") { type = NavType.StringType })) { entry ->
            val session = Session.valueOf(entry.arguments?.getString("session") ?: Session.MORNING.name)
            DoneScreen(
                session = session,
                onHome = goHome,
                onRedo = { navController.navigate(Routes.check(session)) { popUpTo(Routes.HOME) } },
                onHelp = goHelp,
            )
        }

        composable(Routes.EMERGENCY) {
            EmergencyScreen(onBack = { navController.popBackStack() })
        }
    }
}

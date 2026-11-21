package com.onradar.todak.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    // answer: 방금 저장한 답 (-1이면 모름, 예: 홈의 [이야기 나누기]로 들어온 경우)
    const val CHAT = "chat/{session}/{checkId}?answer={answer}"
    const val DONE = "done/{session}?answer={answer}"
    const val EMERGENCY = "emergency"

    fun check(session: Session) = "check/${session.name}"
    fun chat(session: Session, checkId: Long, answer: Int = NO_ANSWER) = "chat/${session.name}/$checkId?answer=$answer"
    fun done(session: Session, answer: Int = NO_ANSWER) = "done/${session.name}?answer=$answer"
    const val NO_ANSWER = -1
}

private val answerArg = navArgument("answer") {
    type = NavType.IntType
    defaultValue = Routes.NO_ANSWER
}

@Composable
fun TodakNavHost(navController: NavHostController = rememberNavController()) {
    val sessionStore = appContainer().sessionStore
    val start = if (sessionStore.isRegistered) Routes.HOME else Routes.ONBOARDING
    val goHelp = { navController.navigate(Routes.EMERGENCY) }
    val goHome = {
        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
    }

    // 서버가 토큰을 거부하면(401) 복지사 도움을 받아 다시 등록하도록 등록 화면으로 보냅니다.
    val expired by sessionStore.expired.collectAsStateWithLifecycle()
    LaunchedEffect(expired) {
        if (expired) {
            navController.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(reRegister = expired, onRegistered = {
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
                onSubmitted = { checkId, answer ->
                    navController.navigate(Routes.chat(session, checkId, answer)) {
                        popUpTo(Routes.HOME) // 대화에서 뒤로 가면 체크가 아니라 홈으로
                    }
                },
                onHelp = goHelp,
            )
        }

        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("session") { type = NavType.StringType },
                navArgument("checkId") { type = NavType.LongType },
                answerArg,
            ),
        ) { entry ->
            val session = Session.valueOf(entry.arguments?.getString("session") ?: Session.MORNING.name)
            val checkId = entry.arguments?.getLong("checkId") ?: 0L
            val answer = entry.arguments?.getInt("answer") ?: Routes.NO_ANSWER
            ChatScreen(
                checkId = checkId,
                onFinished = {
                    navController.navigate(Routes.done(session, answer)) { popUpTo(Routes.HOME) }
                },
                onHelp = goHelp,
            )
        }

        composable(Routes.DONE, arguments = listOf(navArgument("session") { type = NavType.StringType }, answerArg)) { entry ->
            val session = Session.valueOf(entry.arguments?.getString("session") ?: Session.MORNING.name)
            val answer = entry.arguments?.getInt("answer")?.takeIf { it != Routes.NO_ANSWER }
            DoneScreen(
                session = session,
                answer = answer,
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

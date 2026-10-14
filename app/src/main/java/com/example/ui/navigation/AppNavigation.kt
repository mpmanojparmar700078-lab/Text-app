package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.AppContainer
import com.example.ui.admin.AdminPdfScreen
import com.example.ui.admin.AdminPdfViewModel
import com.example.ui.exam.ExamScreen
import com.example.ui.exam.ExamViewModel
import com.example.ui.examsetup.ExamSetupScreen
import com.example.ui.examsetup.ExamSetupViewModel
import com.example.ui.history.TestHistoryScreen
import com.example.ui.history.TestHistoryViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.library.PdfLibraryScreen
import com.example.ui.library.PdfLibraryViewModel
import com.example.ui.preview.QuestionPreviewScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.ProfileViewModel
import com.example.ui.result.ResultScreen
import com.example.ui.result.ResultViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.startexam.StartExamScreen

object AppRoutes {
    const val HOME = "home"
    const val PROFILES = "profiles"
    const val MY_PDFS = "my_pdfs"
    const val ADMIN_LIBRARY = "admin_library"
    const val START_EXAM = "start_exam"
    const val EXAM_SETUP = "exam_setup/{pdfId}"
    const val QUESTION_PREVIEW = "question_preview/{pdfId}"
    const val EXAM = "exam/{pdfId}/{questionCount}"
    const val RESULT = "result/{attemptId}"
    const val TEST_HISTORY = "test_history"
    const val SETTINGS = "settings"

    fun examSetup(pdfId: String) = "exam_setup/$pdfId"
    fun questionPreview(pdfId: String) = "question_preview/$pdfId"
    fun exam(pdfId: String, count: Int) = "exam/$pdfId/$count"
    fun result(attemptId: String) = "result/$attemptId"
}

@Composable
fun AppNavigation(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            val viewModel = remember { HomeViewModel(container) }
            HomeScreen(
                viewModel = viewModel,
                onNavigateToStartExam = { navController.navigate(AppRoutes.START_EXAM) },
                onNavigateToMyPdfs = { navController.navigate(AppRoutes.MY_PDFS) },
                onNavigateToAdminLibrary = { navController.navigate(AppRoutes.ADMIN_LIBRARY) },
                onNavigateToTestHistory = { navController.navigate(AppRoutes.TEST_HISTORY) },
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) },
                onNavigateToProfiles = { navController.navigate(AppRoutes.PROFILES) },
                onSelectPdfForExam = { pdfId -> navController.navigate(AppRoutes.examSetup(pdfId)) }
            )
        }

        composable(AppRoutes.PROFILES) {
            val viewModel = remember { ProfileViewModel(container) }
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.MY_PDFS) {
            val viewModel = remember { PdfLibraryViewModel(container) }
            PdfLibraryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectPdfForExam = { pdfId -> navController.navigate(AppRoutes.examSetup(pdfId)) }
            )
        }

        composable(AppRoutes.ADMIN_LIBRARY) {
            val viewModel = remember { AdminPdfViewModel(container) }
            AdminPdfScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectPdfForExam = { pdfId -> navController.navigate(AppRoutes.examSetup(pdfId)) }
            )
        }

        composable(AppRoutes.START_EXAM) {
            StartExamScreen(
                container = container,
                onNavigateBack = { navController.popBackStack() },
                onSelectPdf = { pdfId -> navController.navigate(AppRoutes.examSetup(pdfId)) },
                onNavigateToMyPdfs = { navController.navigate(AppRoutes.MY_PDFS) }
            )
        }

        composable(
            route = AppRoutes.EXAM_SETUP,
            arguments = listOf(navArgument("pdfId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pdfId = backStackEntry.arguments?.getString("pdfId") ?: ""
            val viewModel = remember(pdfId) { ExamSetupViewModel(container, pdfId) }
            ExamSetupScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { id -> navController.navigate(AppRoutes.questionPreview(id)) },
                onNavigateToExam = { id, count -> navController.navigate(AppRoutes.exam(id, count)) },
                onNavigateToSettings = { navController.navigate(AppRoutes.SETTINGS) }
            )
        }

        composable(
            route = AppRoutes.QUESTION_PREVIEW,
            arguments = listOf(navArgument("pdfId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pdfId = backStackEntry.arguments?.getString("pdfId") ?: ""
            QuestionPreviewScreen(
                container = container,
                pdfId = pdfId,
                onNavigateBack = { navController.popBackStack() },
                onStartExam = { id, count -> navController.navigate(AppRoutes.exam(id, count)) }
            )
        }

        composable(
            route = AppRoutes.EXAM,
            arguments = listOf(
                navArgument("pdfId") { type = NavType.StringType },
                navArgument("questionCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val pdfId = backStackEntry.arguments?.getString("pdfId") ?: ""
            val count = backStackEntry.arguments?.getInt("questionCount") ?: 10
            val viewModel = remember(pdfId, count) { ExamViewModel(container, pdfId, count) }
            ExamScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onExamSubmitted = { attemptId ->
                    navController.navigate(AppRoutes.result(attemptId)) {
                        popUpTo(AppRoutes.HOME)
                    }
                }
            )
        }

        composable(
            route = AppRoutes.RESULT,
            arguments = listOf(navArgument("attemptId") { type = NavType.StringType })
        ) { backStackEntry ->
            val attemptId = backStackEntry.arguments?.getString("attemptId") ?: ""
            val viewModel = remember(attemptId) { ResultViewModel(container, attemptId) }
            ResultScreen(
                viewModel = viewModel,
                onNavigateBackToHome = {
                    navController.navigate(AppRoutes.HOME) {
                        popUpTo(AppRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoutes.TEST_HISTORY) {
            val viewModel = remember { TestHistoryViewModel(container) }
            TestHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenResult = { attemptId -> navController.navigate(AppRoutes.result(attemptId)) }
            )
        }

        composable(AppRoutes.SETTINGS) {
            val viewModel = remember { SettingsViewModel(container) }
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfiles = { navController.navigate(AppRoutes.PROFILES) }
            )
        }
    }
}

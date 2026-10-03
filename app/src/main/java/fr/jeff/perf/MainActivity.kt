package fr.jeff.perf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.jeff.perf.data.PerfRepository
import fr.jeff.perf.ui.EtatApp
import fr.jeff.perf.ui.Routes
import fr.jeff.perf.ui.screens.ExercicesScreen
import fr.jeff.perf.ui.screens.FicheScreen
import fr.jeff.perf.ui.screens.GestionScreen
import fr.jeff.perf.ui.screens.JournalScreen
import fr.jeff.perf.ui.screens.PoidsScreen
import fr.jeff.perf.ui.screens.SaisieScreen
import fr.jeff.perf.ui.theme.PerfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as PerfApp).repository
        setContent {
            PerfTheme { PerfNavigation(repo) }
        }
    }
}

@Composable
private fun PerfNavigation(repo: PerfRepository) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val etat = remember { EtatApp(repo, snackbar, scope) }

    NavHost(navController = nav, startDestination = Routes.EXERCICES) {
        composable(Routes.EXERCICES) { ExercicesScreen(etat, nav) }
        composable(Routes.JOURNAL) { JournalScreen(etat, nav) }
        composable(Routes.POIDS) { PoidsScreen(etat, nav) }
        composable(Routes.GESTION) { GestionScreen(etat, nav) }
        composable(
            route = "${Routes.FICHE}/{exId}",
            arguments = listOf(navArgument("exId") { type = NavType.LongType }),
        ) { entree ->
            FicheScreen(etat, nav, entree.arguments?.getLong("exId") ?: -1L)
        }
        composable(
            route = "${Routes.SAISIE}?perfId={perfId}&exId={exId}",
            arguments = listOf(
                navArgument("perfId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("exId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entree ->
            SaisieScreen(
                etat = etat,
                nav = nav,
                perfId = entree.arguments?.getLong("perfId")?.takeIf { it > 0 },
                exIdInitial = entree.arguments?.getLong("exId")?.takeIf { it > 0 },
            )
        }
    }
}

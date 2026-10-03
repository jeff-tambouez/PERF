package fr.jeff.perf.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import fr.jeff.perf.data.PerfRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** État partagé par tous les écrans : dépôt de données, messages et annulation. */
class EtatApp(
    val repo: PerfRepository,
    val snackbar: SnackbarHostState,
    val scope: CoroutineScope,
) {
    fun message(texte: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(texte, duration = SnackbarDuration.Short)
        }
    }

    /** Affiche « message » avec un bouton Annuler pendant 5 s. */
    fun annulable(texte: String, annuler: suspend () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val affichage = launch {
                val resultat = snackbar.showSnackbar(
                    message = texte,
                    actionLabel = "Annuler",
                    duration = SnackbarDuration.Indefinite,
                )
                if (resultat == SnackbarResult.ActionPerformed) annuler()
            }
            delay(5_000)
            if (affichage.isActive) snackbar.currentSnackbarData?.dismiss()
        }
    }
}

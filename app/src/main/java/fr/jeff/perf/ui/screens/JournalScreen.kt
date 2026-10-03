@file:OptIn(ExperimentalMaterial3Api::class)

package fr.jeff.perf.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.jeff.perf.domain.Format
import fr.jeff.perf.ui.EtatApp
import fr.jeff.perf.ui.Routes
import fr.jeff.perf.ui.components.BarreNavigation

/** Toutes les saisies, groupées par date (la plus récente en haut). Un appui ouvre la saisie en modification. */
@Composable
fun JournalScreen(etat: EtatApp, nav: NavController) {
    val exercices by remember { etat.repo.exercices() }.collectAsState(initial = emptyList())
    val performances by remember { etat.repo.performances() }.collectAsState(initial = null)
    val parId = remember(exercices) { exercices.associateBy { it.id } }
    val parDate = remember(performances) { performances.orEmpty().groupBy { it.date }.toList() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Journal") }) },
        bottomBar = { BarreNavigation(nav, Routes.JOURNAL) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.nouvelleSaisie()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle saisie")
            }
        },
        snackbarHost = { SnackbarHost(etat.snackbar) },
    ) { padding ->
        if (performances != null && parDate.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aucune saisie", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        ) {
            parDate.forEach { (date, liste) ->
                item(key = "date-$date") {
                    Text(
                        Format.dateLongue(date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                    )
                }
                items(liste, key = { it.id }) { p ->
                    val e = parId[p.exerciceId]
                    if (e != null) {
                        LigneHistorique(e, p, titre = e.nom) { nav.navigate(Routes.modifierSaisie(p.id)) }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

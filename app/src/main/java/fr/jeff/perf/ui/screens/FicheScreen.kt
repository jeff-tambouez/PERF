@file:OptIn(ExperimentalMaterial3Api::class)

package fr.jeff.perf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.jeff.perf.data.Exercice
import fr.jeff.perf.data.Performance
import fr.jeff.perf.data.TypeExercice
import fr.jeff.perf.domain.Calculs
import fr.jeff.perf.domain.Format
import fr.jeff.perf.ui.EtatApp
import fr.jeff.perf.ui.Routes
import fr.jeff.perf.ui.components.CarteValeur
import fr.jeff.perf.ui.components.ChoixPeriode
import fr.jeff.perf.ui.components.Courbe
import fr.jeff.perf.ui.components.Legende
import fr.jeff.perf.ui.components.Periode
import fr.jeff.perf.ui.components.Serie
import fr.jeff.perf.ui.components.dansPeriode
import java.time.LocalDate

private enum class Graphique { CHARGE, REPS }

@Composable
fun FicheScreen(etat: EtatApp, nav: NavController, exId: Long) {
    val exercice by remember(exId) { etat.repo.exercice(exId) }.collectAsState(initial = null)
    val perfs by remember(exId) { etat.repo.performancesDe(exId) }.collectAsState(initial = emptyList())
    val pesees by remember { etat.repo.pesees() }.collectAsState(initial = emptyList())

    var graphique by remember { mutableStateOf(Graphique.CHARGE) }
    var periode by remember { mutableStateOf(Periode.TROIS_MOIS) }
    var chargeChoisie by remember { mutableStateOf<Double?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercice?.nom ?: "") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.nouvelleSaisie(exId)) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle saisie")
            }
        },
        snackbarHost = { SnackbarHost(etat.snackbar) },
    ) { padding ->
        val e = exercice ?: return@Scaffold
        // Charge du graphique des reps : choix de l'utilisateur, sinon celle de la dernière série à plusieurs reps.
        val chargeReps = chargeChoisie ?: perfs.firstOrNull { it.reps > 1 }?.chargeKg ?: 0.0
        val entete = remember(e, perfs, pesees) { Calculs.entete(e, perfs, pesees, LocalDate.now()) }

        val accent = MaterialTheme.colorScheme.primary
        val gris = MaterialTheme.colorScheme.onSurfaceVariant
        val unite: (Double) -> String = { v ->
            if (graphique == Graphique.REPS) Format.nombre(v) else Format.enKg(v)
        }
        val series = remember(e, perfs, pesees, graphique, periode, chargeReps, accent, gris) {
            when (graphique) {
                Graphique.CHARGE -> listOf(
                    Serie(
                        nom = if (e.type == TypeExercice.BARRE) "1RM réel" else "1RM réel (lest)",
                        points = Calculs.serie1RMReel(e, perfs).dansPeriode(periode),
                        couleur = accent,
                        epaisseur = 2.5.dp,
                    ),
                    Serie(
                        nom = "1RM estimé",
                        points = Calculs.serie1RMEstime(e, perfs, pesees).dansPeriode(periode),
                        couleur = gris,
                        pointille = true,
                        epaisseur = 1.5.dp,
                    ),
                )
                Graphique.REPS -> listOf(
                    Serie(
                        nom = "Reps max à ${Format.charge(e, chargeReps)}",
                        points = Calculs.serieReps(perfs, chargeReps).dansPeriode(periode),
                        couleur = accent,
                        epaisseur = 2.5.dp,
                    ),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CarteValeur("1RM · 90 j", entete.rm1, e, Modifier.weight(1f))
                    CarteValeur("5 reps · 90 j", entete.rm5, e, Modifier.weight(1f))
                }
                if (entete.ancien) {
                    Text(
                        "Aucune saisie depuis 90 jours : valeurs calculées sur les 90 jours précédant la dernière saisie.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = graphique == Graphique.CHARGE,
                        onClick = { graphique = Graphique.CHARGE },
                        label = { Text("Charge") },
                    )
                    FilterChip(
                        selected = graphique == Graphique.REPS,
                        onClick = { graphique = Graphique.REPS },
                        label = { Text("Reps") },
                    )
                    if (graphique == Graphique.REPS) {
                        ChoixChargeReps(e, perfs, chargeReps) { chargeChoisie = it }
                    }
                }
                ChoixPeriode(
                    periode, { periode = it },
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                Courbe(series, unite)
                Legende(series)
                Spacer(Modifier.height(24.dp))
                Text("Historique", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (perfs.isEmpty()) {
                    Text(
                        "Aucune saisie pour cet exercice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }

            items(perfs, key = { it.id }) { p ->
                LigneHistorique(e, p) { nav.navigate(Routes.modifierSaisie(p.id)) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ChoixChargeReps(e: Exercice, perfs: List<Performance>, choisie: Double, onChoix: (Double) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    val charges = remember(perfs) {
        (listOf(0.0) + perfs.map { it.chargeKg }.filter { it > 0 }.distinct().sorted())
    }
    Box {
        FilterChip(
            selected = true,
            onClick = { ouvert = true },
            label = { Text(Format.charge(e, choisie)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            charges.forEach { c ->
                DropdownMenuItem(
                    text = { Text(Format.charge(e, c)) },
                    onClick = { onChoix(c); ouvert = false },
                )
            }
        }
    }
}

@Composable
fun LigneHistorique(e: Exercice, p: Performance, titre: String = Format.date(p.date), onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            titre,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${Format.reps(p.reps)} · ${Format.charge(e, p.chargeKg)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

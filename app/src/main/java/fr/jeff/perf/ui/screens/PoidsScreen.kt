@file:OptIn(ExperimentalMaterial3Api::class)

package fr.jeff.perf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import fr.jeff.perf.data.Pesee
import fr.jeff.perf.domain.Calculs
import fr.jeff.perf.domain.Format
import fr.jeff.perf.ui.EtatApp
import fr.jeff.perf.ui.Routes
import fr.jeff.perf.ui.components.BarreNavigation
import fr.jeff.perf.ui.components.ChampNombre
import fr.jeff.perf.ui.components.ChoixDate
import fr.jeff.perf.ui.components.ChoixPeriode
import fr.jeff.perf.ui.components.Courbe
import fr.jeff.perf.ui.components.Legende
import fr.jeff.perf.ui.components.Periode
import fr.jeff.perf.ui.components.Serie
import fr.jeff.perf.ui.components.dansPeriode
import fr.jeff.perf.ui.components.libelleDate
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Pesée en cours d'édition dans la boîte de saisie (id = 0 pour une nouvelle pesée). */
private data class Edition(val pesee: Pesee)

@Composable
fun PoidsScreen(etat: EtatApp, nav: NavController) {
    val pesees by remember { etat.repo.pesees() }.collectAsState(initial = emptyList())
    var periode by remember { mutableStateOf(Periode.TROIS_MOIS) }
    var edition by remember { mutableStateOf<Edition?>(null) }

    val accent = MaterialTheme.colorScheme.primary
    val gris = MaterialTheme.colorScheme.onSurfaceVariant
    val series = remember(pesees, periode, accent, gris) {
        listOf(
            Serie(
                nom = "Pesées",
                points = pesees.map { Calculs.Point(it.date, it.poidsKg) }.dansPeriode(periode),
                couleur = gris,
                ligne = false,
            ),
            Serie(
                nom = "Moyenne 7 jours",
                points = Calculs.moyenneGlissante(pesees).dansPeriode(periode),
                couleur = accent,
                pointsVisibles = false,
                epaisseur = 3.dp,
            ),
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Poids de corps") }) },
        bottomBar = { BarreNavigation(nav, Routes.POIDS) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val dernier = pesees.lastOrNull()?.poidsKg ?: Calculs.PDC_DEFAUT
                edition = Edition(Pesee(date = LocalDate.now(), poidsKg = dernier))
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle pesée")
            }
        },
        snackbarHost = { SnackbarHost(etat.snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        ) {
            item {
                EnTetePoids(pesees)
                Spacer(Modifier.height(16.dp))
                ChoixPeriode(periode, { periode = it }, Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()))
                Spacer(Modifier.height(12.dp))
                Courbe(series, { Format.enKg(it) })
                Legende(series)
                Spacer(Modifier.height(24.dp))
                Text("Historique", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (pesees.isEmpty()) {
                    Text(
                        "Aucune pesée. Les calculs au poids du corps utilisent 80 kg.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            items(pesees.reversed(), key = { it.id }) { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { edition = Edition(p) }
                        .padding(vertical = 14.dp),
                ) {
                    Text(
                        Format.dateAvecAnnee(p.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(Format.enKg(p.poidsKg), style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    edition?.let { ed ->
        DialoguePesee(
            initiale = ed.pesee,
            onFermer = { edition = null },
            onEnregistrer = { p ->
                edition = null
                etat.scope.launch { etat.repo.enregistrer(p) }
                etat.message("Pesée enregistrée")
            },
            onSupprimer = if (ed.pesee.id != 0L) {
                {
                    edition = null
                    val p = ed.pesee
                    etat.scope.launch { etat.repo.supprimer(p) }
                    etat.annulable("Pesée supprimée") { etat.repo.enregistrer(p) }
                }
            } else null,
        )
    }
}

@Composable
private fun EnTetePoids(pesees: List<Pesee>) {
    val derniere = pesees.lastOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Dernière pesée", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (derniere == null) {
                Text("Pas encore de donnée", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(
                    Format.enKg(derniere.poidsKg),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                // Écart sur 30 jours : par rapport à la dernière pesée d'il y a au moins 30 jours, sinon à la première.
                val reference = pesees.lastOrNull { !it.date.isAfter(derniere.date.minusDays(30)) }
                    ?: pesees.first().takeIf { it.id != derniere.id }
                val detail = buildString {
                    append(Format.date(derniere.date))
                    if (reference != null) {
                        append(" · ")
                        append(Format.ecart(derniere.poidsKg - reference.poidsKg))
                        append(" depuis le ")
                        append(Format.date(reference.date))
                    }
                }
                Text(detail, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DialoguePesee(
    initiale: Pesee,
    onFermer: () -> Unit,
    onEnregistrer: (Pesee) -> Unit,
    onSupprimer: (() -> Unit)?,
) {
    var date by remember { mutableStateOf(initiale.date) }
    var texte by remember { mutableStateOf(Format.nombre(initiale.poidsKg)) }
    var choixDate by remember { mutableStateOf(false) }
    val poids = Format.parseNombre(texte)
    val erreur = when {
        poids == null -> "Nombre attendu"
        poids < 30.0 || poids > 250.0 -> "Entre 30 et 250 kg"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(if (initiale.id == 0L) "Nouvelle pesée" else "Modifier la pesée") },
        text = {
            Column {
                OutlinedButton(onClick = { choixDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Date : ${libelleDate(date)}", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                ChampNombre(
                    label = "Poids",
                    valeur = texte,
                    onChange = { texte = it },
                    pas = 0.1,
                    entier = false,
                    suffixe = "kg",
                    erreur = erreur,
                )
                Text(
                    "Une pesée par jour : une pesée existante à cette date sera remplacée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = erreur == null && poids != null,
                onClick = { if (poids != null) onEnregistrer(initiale.copy(date = date, poidsKg = poids)) },
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onSupprimer != null) {
                    TextButton(onClick = onSupprimer) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onFermer) { Text("Annuler") }
            }
        },
    )

    if (choixDate) {
        ChoixDate(date = date, onChoix = { date = it }, onFermer = { choixDate = false })
    }
}

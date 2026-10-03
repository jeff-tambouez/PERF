@file:OptIn(ExperimentalMaterial3Api::class)

package fr.jeff.perf.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.jeff.perf.data.Exercice
import fr.jeff.perf.domain.Calculs
import fr.jeff.perf.domain.Format
import fr.jeff.perf.ui.Routes
import java.time.LocalDate

// ---------- Barre de navigation du bas ----------

@Composable
fun BarreNavigation(nav: NavController, courant: String) {
    NavigationBar {
        NavigationBarItem(
            selected = courant == Routes.EXERCICES,
            onClick = { aller(nav, courant, Routes.EXERCICES) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Exercices") },
        )
        NavigationBarItem(
            selected = courant == Routes.JOURNAL,
            onClick = { aller(nav, courant, Routes.JOURNAL) },
            icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            label = { Text("Journal") },
        )
        NavigationBarItem(
            selected = courant == Routes.POIDS,
            onClick = { aller(nav, courant, Routes.POIDS) },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Poids") },
        )
    }
}

private fun aller(nav: NavController, courant: String, cible: String) {
    if (courant == cible) return
    nav.navigate(cible) {
        popUpTo(Routes.EXERCICES) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// ---------- Valeurs d'en-tête (1RM, 5 reps) ----------

/** Grande carte de l'en-tête de la fiche. Réel : fond accent. Estimé : contour, chiffre en gris. */
@Composable
fun CarteValeur(titre: String, valeur: Calculs.Valeur?, exercice: Exercice, modifier: Modifier = Modifier) {
    val reelle = valeur != null && !valeur.estimee
    val contenu: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp)) {
            Text(titre, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (valeur == null) {
                Text(
                    "Pas encore de donnée",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Text(
                    Format.chargeCourte(exercice, valeur.kg),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (reelle) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    if (valeur.estimee) "estimé" else valeur.date?.let { Format.date(it) } ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    fontStyle = if (valeur.estimee) FontStyle.Italic else FontStyle.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Format.detailBarre(exercice, valeur.kg)?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                valeur.reel?.let { r ->
                    Text(
                        "réel : ${Format.chargeCourte(exercice, r.kg)}" + (r.date?.let { " (${Format.date(it)})" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (reelle) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) { contenu() }
    } else {
        OutlinedCard(modifier = modifier) { contenu() }
    }
}

/** Version compacte pour la liste des exercices. */
fun texteCompact(valeur: Calculs.Valeur?, exercice: Exercice): String = when {
    valeur == null -> "Pas de donnée"
    valeur.estimee -> "${Format.chargeCourte(exercice, valeur.kg)} (estimé)"
    else -> Format.chargeCourte(exercice, valeur.kg)
}

// ---------- Champ numérique avec boutons − / + ----------

@Composable
fun ChampNombre(
    label: String,
    valeur: String,
    onChange: (String) -> Unit,
    pas: Double,
    entier: Boolean,
    suffixe: String,
    erreur: String?,
    modifier: Modifier = Modifier,
    minimum: Double = 0.0,
) {
    fun decaler(delta: Double) {
        val actuel = Format.parseNombre(valeur) ?: 0.0
        val nouveau = (actuel + delta).coerceAtLeast(minimum)
        onChange(if (entier) nouveau.toInt().toString() else Format.nombre(nouveau))
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(
            onClick = { decaler(-pas) },
            modifier = Modifier.size(52.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        OutlinedTextField(
            value = valeur,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            suffix = { Text(suffixe) },
            isError = erreur != null,
            supportingText = if (erreur != null) { { Text(erreur) } } else null,
            keyboardOptions = KeyboardOptions(keyboardType = if (entier) KeyboardType.Number else KeyboardType.Decimal),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        FilledTonalButton(
            onClick = { decaler(pas) },
            modifier = Modifier.size(52.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

// ---------- Sélecteur de date (pas de date future) ----------

private const val MS_PAR_JOUR = 86_400_000L

@Composable
fun ChoixDate(date: LocalDate, onChoix: (LocalDate) -> Unit, onFermer: () -> Unit) {
    val aujourdhui = LocalDate.now()
    val etat = rememberDatePickerState(
        initialSelectedDateMillis = date.toEpochDay() * MS_PAR_JOUR,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis / MS_PAR_JOUR <= aujourdhui.toEpochDay()

            override fun isSelectableYear(year: Int): Boolean = year <= aujourdhui.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onFermer,
        confirmButton = {
            TextButton(onClick = {
                etat.selectedDateMillis?.let { onChoix(LocalDate.ofEpochDay(it / MS_PAR_JOUR)) }
                onFermer()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } },
    ) {
        DatePicker(state = etat)
    }
}

fun libelleDate(date: LocalDate): String {
    val aujourdhui = LocalDate.now()
    return when (date) {
        aujourdhui -> "Aujourd'hui"
        aujourdhui.minusDays(1) -> "Hier"
        else -> Format.dateAvecAnnee(date)
    }
}

// ---------- Période des courbes ----------

enum class Periode(val libelle: String, val jours: Long?) {
    UN_MOIS("1 mois", 30),
    TROIS_MOIS("3 mois", 91),
    UN_AN("1 an", 365),
    TOUT("Tout", null),
}

fun List<Calculs.Point>.dansPeriode(periode: Periode): List<Calculs.Point> {
    val jours = periode.jours ?: return this
    val debut = LocalDate.now().minusDays(jours)
    return filter { !it.date.isBefore(debut) }
}

@Composable
fun ChoixPeriode(periode: Periode, onChoix: (Periode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Periode.entries.forEach { p ->
            FilterChip(
                selected = p == periode,
                onClick = { onChoix(p) },
                label = { Text(p.libelle) },
            )
        }
    }
}

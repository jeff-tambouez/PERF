@file:OptIn(ExperimentalMaterial3Api::class)

package fr.jeff.perf.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.jeff.perf.data.Exercice
import fr.jeff.perf.data.Performance
import fr.jeff.perf.data.estBarre
import fr.jeff.perf.domain.Format
import fr.jeff.perf.ui.EtatApp
import fr.jeff.perf.ui.components.ChampNombre
import fr.jeff.perf.ui.components.ChoixDate
import fr.jeff.perf.ui.components.libelleDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.round

private enum class Mode(val libelle: String) {
    UN_RM("1RM"),
    REPS_CHARGE("Reps avec charge"),
    REPS_SANS("Reps sans charge"),
}

@Composable
fun SaisieScreen(etat: EtatApp, nav: NavController, perfId: Long?, exIdInitial: Long?) {
    val exercices by remember { etat.repo.exercices() }.collectAsState(initial = null)
    val performances by remember { etat.repo.performances() }.collectAsState(initial = null)

    var original by remember { mutableStateOf<Performance?>(null) }
    var exId by remember { mutableStateOf(exIdInitial) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var mode by remember { mutableStateOf(Mode.REPS_CHARGE) }
    /** Barre : total des disques. Poids du corps : lest. */
    var chargeTexte by remember { mutableStateOf("") }
    var repsTexte by remember { mutableStateOf("") }
    var prerempliPour by remember { mutableStateOf<Long?>(null) }
    var choixDateOuvert by remember { mutableStateOf(false) }
    var menuExercice by remember { mutableStateOf(false) }
    /** Évite un double enregistrement ou un double retour pendant l'animation de sortie. */
    var termine by remember { mutableStateOf(false) }

    val enModification = perfId != null
    val aujourdhui = LocalDate.now()

    // Chargement de la saisie à modifier
    LaunchedEffect(perfId) {
        if (perfId != null) {
            val p = etat.repo.performance(perfId)
            if (p == null) {
                nav.popBackStack()
            } else {
                original = p
                exId = p.exerciceId
                date = p.date
            }
        }
    }

    val exercice: Exercice? = exercices?.firstOrNull { it.id == exId }

    // Pré-remplissage : la saisie modifiée, sinon la dernière saisie de l'exercice choisi.
    LaunchedEffect(exercice?.id, performances, original) {
        val e = exercice ?: return@LaunchedEffect
        if (prerempliPour == e.id) return@LaunchedEffect
        if (enModification && prerempliPour != null) return@LaunchedEffect // on garde les valeurs si l'exercice change
        val o = original
        val source: Performance? = when {
            enModification -> o ?: return@LaunchedEffect
            else -> (performances ?: return@LaunchedEffect).firstOrNull { it.exerciceId == e.id }
        }
        if (source == null) {
            mode = if (e.estBarre) Mode.REPS_CHARGE else Mode.REPS_SANS
            chargeTexte = "0"
            repsTexte = "5"
        } else {
            mode = when {
                source.chargeKg <= 0.0 -> Mode.REPS_SANS
                source.reps == 1 -> Mode.UN_RM
                else -> Mode.REPS_CHARGE
            }
            val affiche = if (e.estBarre) (source.chargeKg - e.poidsBarreKg).coerceAtLeast(0.0) else source.chargeKg
            chargeTexte = Format.nombre(affiche)
            repsTexte = source.reps.toString()
        }
        prerempliPour = e.id
    }

    // ---- Calcul et contrôles ----
    val chargeSaisie = Format.parseNombre(chargeTexte)
    val repsSaisies = repsTexte.trim().toIntOrNull()
    val total: Double? = when {
        exercice == null -> null
        mode == Mode.REPS_SANS -> 0.0
        chargeSaisie == null -> null
        exercice.estBarre -> exercice.poidsBarreKg + chargeSaisie
        else -> chargeSaisie
    }
    val repsFinales: Int? = if (mode == Mode.UN_RM) 1 else repsSaisies

    val erreurCharge: String? = when {
        mode == Mode.REPS_SANS || exercice == null -> null
        chargeSaisie == null -> "Nombre attendu"
        chargeSaisie < 0 -> "Valeur positive attendue"
        (total ?: 0.0) > 500.0 -> "500 kg maximum au total"
        abs(chargeSaisie * 2 - round(chargeSaisie * 2)) > 1e-6 -> "Par pas de 0,5 kg"
        !exercice.estBarre && chargeSaisie <= 0.0 -> "Lest > 0 (sinon : Reps sans charge)"
        else -> null
    }
    val erreurReps: String? = when {
        mode == Mode.UN_RM -> null
        repsSaisies == null -> "Nombre entier attendu"
        repsSaisies !in 1..500 -> "Entre 1 et 500"
        else -> null
    }
    val valide = exercice != null && total != null && repsFinales != null &&
        erreurCharge == null && erreurReps == null && !date.isAfter(aujourdhui)

    fun fermer() {
        if (termine) return
        termine = true
        nav.popBackStack()
    }

    fun enregistrer() {
        if (termine) return
        val e = exercice ?: return
        val t = total ?: return
        val r = repsFinales ?: return
        val o = original
        val p = Performance(
            id = o?.id ?: 0,
            exerciceId = e.id,
            date = date,
            chargeKg = t,
            reps = r,
            creeLe = o?.creeLe ?: System.currentTimeMillis(),
        )
        etat.scope.launch { etat.repo.enregistrer(p) }
        fermer()
        etat.message(if (o != null) "Saisie modifiée" else "Saisie enregistrée")
    }

    fun supprimer() {
        if (termine) return
        val o = original ?: return
        etat.scope.launch { etat.repo.supprimer(o) }
        fermer()
        etat.annulable("Saisie supprimée") { etat.repo.enregistrer(o) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (enModification) "Modifier la saisie" else "Nouvelle saisie") },
                navigationIcon = {
                    IconButton(onClick = { fermer() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                },
                actions = {
                    if (enModification) {
                        IconButton(onClick = { supprimer() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                        }
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { enregistrer() },
                enabled = valide,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
                    .height(52.dp),
            ) { Text("Enregistrer") }
        },
        snackbarHost = { SnackbarHost(etat.snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Date
            OutlinedButton(onClick = { choixDateOuvert = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Date : ${libelleDate(date)}", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))

            // Exercice
            Box {
                OutlinedButton(onClick = { menuExercice = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(exercice?.nom ?: "Choisir un exercice", modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = menuExercice, onDismissRequest = { menuExercice = false }) {
                    exercices.orEmpty().filter { !it.archive || it.id == exId }.forEach { e ->
                        DropdownMenuItem(
                            text = { Text(e.nom) },
                            onClick = { exId = e.id; menuExercice = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (exercice != null) {
                // Mode
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Mode.entries.forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.libelle) })
                    }
                }
                Spacer(Modifier.height(16.dp))

                if (mode != Mode.REPS_SANS) {
                    ChampNombre(
                        label = if (exercice.estBarre) "Disques (total)" else "Lest",
                        valeur = chargeTexte,
                        onChange = { chargeTexte = it },
                        pas = 2.5,
                        entier = false,
                        suffixe = "kg",
                        erreur = erreurCharge,
                    )
                    if (exercice.estBarre) {
                        Text(
                            "Total : " + (total?.let { Format.charge(exercice, it) } ?: "?"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 60.dp, top = 4.dp, bottom = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (mode != Mode.UN_RM) {
                    ChampNombre(
                        label = "Répétitions",
                        valeur = repsTexte,
                        onChange = { repsTexte = it },
                        pas = 1.0,
                        entier = true,
                        suffixe = "reps",
                        erreur = erreurReps,
                        minimum = 1.0,
                    )
                } else {
                    Text(
                        "Une répétition à charge maximale.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (mode == Mode.REPS_SANS) {
                    Text(
                        if (exercice.estBarre) "Sans charge (barre non comptée)." else "Au poids du corps, sans lest.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }

    if (choixDateOuvert) {
        ChoixDate(date = date, onChoix = { date = it }, onFermer = { choixDateOuvert = false })
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package fr.jeff.perf.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.jeff.perf.data.Exercice
import fr.jeff.perf.data.NomDejaUtiliseException
import fr.jeff.perf.data.TypeExercice
import fr.jeff.perf.domain.Format
import fr.jeff.perf.ui.EtatApp
import kotlinx.coroutines.launch

/** Ajouter, renommer, archiver et supprimer les exercices. L'archivage masque sans supprimer l'historique. */
@Composable
fun GestionScreen(etat: EtatApp, nav: NavController) {
    val exercices by remember { etat.repo.exercices() }.collectAsState(initial = emptyList())
    var edition by remember { mutableStateOf<Exercice?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gérer les exercices") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { edition = Exercice(nom = "", type = TypeExercice.BARRE) }) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un exercice")
            }
        },
        snackbarHost = { SnackbarHost(etat.snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp, bottom = 88.dp),
        ) {
            items(exercices, key = { it.id }) { e ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { edition = e }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            e.nom,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (e.archive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            buildString {
                                append(if (e.type == TypeExercice.BARRE) "Barre ${Format.enKg(e.poidsBarreKg)}" else "Poids du corps")
                                if (e.archive) append(" · archivé")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = !e.archive,
                        onCheckedChange = { actif ->
                            etat.scope.launch { etat.repo.modifier(e.copy(archive = !actif)) }
                        },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    edition?.let { e ->
        DialogueExercice(
            initial = e,
            onFermer = { edition = null },
            nombrePerformances = { etat.repo.nombrePerformances(e) },
            onSupprimer = if (e.id != 0L) {
                {
                    etat.repo.supprimer(e)
                    edition = null
                    etat.message("« ${e.nom} » supprimé")
                }
            } else null,
            onEnregistrer = { modifie ->
                if (modifie.id == 0L) etat.repo.ajouter(modifie) else etat.repo.modifier(modifie)
                edition = null
            },
        )
    }
}

@Composable
fun DialogueExercice(
    initial: Exercice,
    onFermer: () -> Unit,
    nombrePerformances: suspend () -> Int,
    onSupprimer: (suspend () -> Unit)?,
    onEnregistrer: suspend (Exercice) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirmation by remember { mutableStateOf<Int?>(null) }
    var nom by remember { mutableStateOf(initial.nom) }
    var type by remember { mutableStateOf(initial.type) }
    var barreTexte by remember { mutableStateOf(Format.nombre(initial.poidsBarreKg)) }
    var erreurNom by remember { mutableStateOf<String?>(null) }
    val poidsBarre = Format.parseNombre(barreTexte)
    val erreurBarre = if (type == TypeExercice.BARRE && (poidsBarre == null || poidsBarre < 0 || poidsBarre > 50)) "Entre 0 et 50 kg" else null

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(if (initial.id == 0L) "Nouvel exercice" else "Modifier l'exercice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it; erreurNom = null },
                    label = { Text("Nom") },
                    singleLine = true,
                    isError = erreurNom != null,
                    supportingText = if (erreurNom != null) { { Text(erreurNom ?: "") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TypeExercice.BARRE,
                        onClick = { type = TypeExercice.BARRE },
                        label = { Text("Barre") },
                    )
                    FilterChip(
                        selected = type == TypeExercice.POIDS_DU_CORPS,
                        onClick = { type = TypeExercice.POIDS_DU_CORPS },
                        label = { Text("Poids du corps") },
                    )
                }
                if (type == TypeExercice.BARRE) {
                    OutlinedTextField(
                        value = barreTexte,
                        onValueChange = { barreTexte = it },
                        label = { Text("Poids de la barre") },
                        suffix = { Text("kg") },
                        singleLine = true,
                        isError = erreurBarre != null,
                        supportingText = if (erreurBarre != null) { { Text(erreurBarre) } } else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = nom.isNotBlank() && erreurBarre == null,
                onClick = {
                    val modifie = initial.copy(
                        nom = nom.trim(),
                        type = type,
                        poidsBarreKg = poidsBarre ?: Exercice.POIDS_BARRE_DEFAUT,
                    )
                    scope.launch {
                        try {
                            onEnregistrer(modifie)
                        } catch (e: NomDejaUtiliseException) {
                            erreurNom = "Ce nom existe déjà"
                        }
                    }
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            Row {
                if (onSupprimer != null) {
                    TextButton(onClick = { scope.launch { confirmation = nombrePerformances() } }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onFermer) { Text("Annuler") }
            }
        },
    )

    val nbPerfs = confirmation
    if (nbPerfs != null && onSupprimer != null) {
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Supprimer « ${initial.nom} » ?") },
            text = {
                Text(
                    if (nbPerfs > 0) "Les $nbPerfs performances enregistrées pour cet exercice seront aussi supprimées définitivement. Pour simplement le masquer, archivez-le plutôt."
                    else "Cet exercice n'a aucune performance enregistrée."
                )
            },
            confirmButton = {
                TextButton(onClick = { scope.launch { onSupprimer() } }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Annuler") } },
        )
    }
}

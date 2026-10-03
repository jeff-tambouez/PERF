@file:OptIn(ExperimentalMaterial3Api::class)

package fr.jeff.perf.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.jeff.perf.domain.Calculs
import fr.jeff.perf.ui.EtatApp
import fr.jeff.perf.ui.Routes
import fr.jeff.perf.ui.components.BarreNavigation
import fr.jeff.perf.ui.components.texteCompact
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ExercicesScreen(etat: EtatApp, nav: NavController) {
    val exercices by remember { etat.repo.exercices() }.collectAsState(initial = null)
    val performances by remember { etat.repo.performances() }.collectAsState(initial = emptyList())
    val pesees by remember { etat.repo.pesees() }.collectAsState(initial = emptyList())
    val resolver = LocalContext.current.contentResolver

    var menuOuvert by remember { mutableStateOf(false) }
    var aImporter by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) etat.scope.launch {
            runCatching { etat.repo.exporter(resolver, uri) }
                .onSuccess { etat.message("Export terminé") }
                .onFailure { etat.message("Échec de l'export : ${it.message}") }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        aImporter = uris
    }

    val actifs = exercices.orEmpty().filter { !it.archive }
    val aujourdhui = LocalDate.now()
    val entetes = remember(actifs, performances, pesees) {
        val parExercice = performances.groupBy { it.exerciceId }
        actifs.associate { e ->
            e.id to Calculs.entete(e, parExercice[e.id].orEmpty(), pesees, aujourdhui)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PERF", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.GESTION) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Gérer les exercices")
                    }
                    Box {
                        IconButton(onClick = { menuOuvert = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Plus")
                        }
                        DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                            DropdownMenuItem(
                                text = { Text("Exporter (ZIP de CSV)") },
                                onClick = {
                                    menuOuvert = false
                                    exporter.launch("perf-${LocalDate.now()}.zip")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Importer (CSV ou ZIP)") },
                                onClick = {
                                    menuOuvert = false
                                    importer.launch(arrayOf("*/*"))
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = { BarreNavigation(nav, Routes.EXERCICES) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.nouvelleSaisie()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nouvelle saisie")
            }
        },
        snackbarHost = { SnackbarHost(etat.snackbar) },
    ) { padding ->
        if (exercices != null && actifs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Aucun exercice actif", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(actifs, key = { it.id }) { e ->
                val entete = entetes[e.id]
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate(Routes.fiche(e.id)) },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(e.nom, style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            ValeurCompacte("1RM", texteCompact(entete?.rm1, e), entete?.rm1?.estimee != false, Modifier.weight(1f))
                            ValeurCompacte("5 reps", texteCompact(entete?.rm5, e), entete?.rm5?.estimee != false, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (aImporter.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { aImporter = emptyList() },
            title = { Text("Importer les données ?") },
            text = { Text("Chaque table trouvée dans les fichiers remplace entièrement les données actuelles de cette table.") },
            confirmButton = {
                TextButton(onClick = {
                    val uris = aImporter
                    aImporter = emptyList()
                    etat.scope.launch {
                        runCatching { etat.repo.importer(resolver, uris) }
                            .onSuccess { etat.message(it) }
                            .onFailure { etat.message("Échec de l'import : ${it.message}") }
                    }
                }) { Text("Importer") }
            },
            dismissButton = { TextButton(onClick = { aImporter = emptyList() }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun ValeurCompacte(titre: String, texte: String, discret: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(titre, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            texte,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (discret) FontWeight.Normal else FontWeight.SemiBold,
            color = if (discret) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
    }
}

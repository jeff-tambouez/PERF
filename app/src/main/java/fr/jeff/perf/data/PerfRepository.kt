package fr.jeff.perf.data

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class NomDejaUtiliseException : Exception("Ce nom d'exercice existe déjà")

private val comparateurNom: Comparator<Exercice> = java.text.Collator.getInstance(java.util.Locale.FRENCH)
    .apply { strength = java.text.Collator.PRIMARY }
    .let { c -> Comparator { a, b -> c.compare(a.nom, b.nom) } }

class PerfRepository(private val db: PerfDatabase) {
    private val exercices = db.exerciceDao()
    private val performances = db.performanceDao()
    private val pesees = db.peseeDao()

    // ---- Lecture ----
    /** Triés par ordre alphabétique (insensible à la casse et aux accents). */
    fun exercices(): Flow<List<Exercice>> = exercices.tous().map { it.sortedWith(comparateurNom) }
    fun exercice(id: Long): Flow<Exercice?> = exercices.parId(id)
    fun performances(): Flow<List<Performance>> = performances.toutes()
    fun performancesDe(exerciceId: Long): Flow<List<Performance>> = performances.parExercice(exerciceId)
    fun pesees(): Flow<List<Pesee>> = pesees.toutes()
    suspend fun performance(id: Long): Performance? = performances.parId(id)

    // ---- Performances ----
    suspend fun enregistrer(p: Performance): Long = performances.enregistrer(p)
    suspend fun supprimer(p: Performance) = performances.supprimer(p)

    // ---- Pesées ----
    /** Une pesée par jour : si la date change vers un jour déjà pesé, l'ancienne pesée de ce jour est remplacée. */
    suspend fun enregistrer(p: Pesee): Long = pesees.enregistrer(p)
    suspend fun supprimer(p: Pesee) = pesees.supprimer(p)

    // ---- Exercices ----
    suspend fun ajouter(e: Exercice): Long = try {
        exercices.inserer(e.copy(id = 0, ordre = exercices.prochainOrdre()))
    } catch (ex: android.database.sqlite.SQLiteConstraintException) {
        throw NomDejaUtiliseException()
    }

    suspend fun modifier(e: Exercice) = try {
        exercices.modifier(e)
    } catch (ex: android.database.sqlite.SQLiteConstraintException) {
        throw NomDejaUtiliseException()
    }

    suspend fun nombrePerformances(e: Exercice): Int = performances.compter(e.id)

    /** Supprime définitivement l'exercice et tout son historique de performances. */
    suspend fun supprimer(e: Exercice) = db.withTransaction {
        performances.supprimerDeExercice(e.id)
        exercices.supprimerParId(e.id)
    }

    // ---- Export / import ----

    /** Écrit un ZIP contenant exercices.csv, performances.csv et poids_corps.csv. */
    suspend fun exporter(resolver: ContentResolver, uri: Uri) = withContext(Dispatchers.IO) {
        val contenus = mapOf(
            Csv.Table.EXERCICES.nomFichier to Csv.ecrireExercices(exercices.tousMaintenant()),
            Csv.Table.PERFORMANCES.nomFichier to Csv.ecrirePerformances(performances.toutesMaintenant()),
            Csv.Table.POIDS_CORPS.nomFichier to Csv.ecrirePesees(pesees.toutesMaintenant()),
        )
        resolver.openOutputStream(uri)?.use { sortie ->
            ZipOutputStream(sortie).use { zip ->
                contenus.forEach { (nom, texte) ->
                    zip.putNextEntry(ZipEntry(nom))
                    zip.write(texte.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        } ?: error("Impossible d'écrire le fichier")
    }

    /**
     * Importe un ou plusieurs fichiers (CSV ou ZIP d'export).
     * Chaque table trouvée remplace entièrement la table correspondante.
     * Retourne un résumé lisible.
     */
    suspend fun importer(resolver: ContentResolver, uris: List<Uri>): String = withContext(Dispatchers.IO) {
        val fichiers = mutableListOf<Csv.Fichier>()
        uris.forEach { uri ->
            val octets = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@forEach
            if (estZip(octets)) {
                ZipInputStream(ByteArrayInputStream(octets)).use { zip ->
                    var entree = zip.nextEntry
                    while (entree != null) {
                        if (!entree.isDirectory && entree.name.endsWith(".csv")) {
                            fichiers += Csv.lire(zip.readBytes().toString(Charsets.UTF_8))
                        }
                        entree = zip.nextEntry
                    }
                }
            } else {
                fichiers += Csv.lire(octets.toString(Charsets.UTF_8))
            }
        }
        val parTable = fichiers.filter { it.table != null }.associateBy { it.table!! }
        if (parTable.isEmpty()) error("Aucun fichier reconnu (en-têtes attendus : exercices, performances ou poids_corps)")

        val listeExercices = parTable[Csv.Table.EXERCICES]?.lignes?.map(Csv::versExercice)
        val listePerformances = parTable[Csv.Table.PERFORMANCES]?.lignes?.map(Csv::versPerformance)
        val listePesees = parTable[Csv.Table.POIDS_CORPS]?.lignes?.map(Csv::versPesee)

        db.withTransaction {
            listeExercices?.let { exercices.toutSupprimer(); exercices.insererTous(it) }
            listePerformances?.let { performances.toutSupprimer(); performances.insererTous(it) }
            listePesees?.let { pesees.toutSupprimer(); pesees.insererTous(it) }
        }
        buildList {
            listeExercices?.let { add("${it.size} exercices") }
            listePerformances?.let { add("${it.size} performances") }
            listePesees?.let { add("${it.size} pesées") }
        }.joinToString(", ", prefix = "Importé : ")
    }

    private fun estZip(octets: ByteArray) =
        octets.size >= 4 && octets[0] == 0x50.toByte() && octets[1] == 0x4B.toByte()
}

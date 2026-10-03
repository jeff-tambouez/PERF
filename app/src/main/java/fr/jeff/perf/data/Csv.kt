package fr.jeff.perf.data

import java.time.LocalDate
import java.util.Locale

/**
 * Lecture et écriture des CSV d'export / import.
 * Formats (en-têtes) :
 *  - exercices    : id,nom,type,archive,ordre[,poids_barre_kg]
 *  - performances : id,exercice_id,date,charge_kg,reps[,cree_le]
 *  - poids_corps  : id,date,poids_kg
 * La table est reconnue par son en-tête, pas par le nom du fichier.
 */
object Csv {

    enum class Table(val nomFichier: String) {
        EXERCICES("exercices.csv"),
        PERFORMANCES("performances.csv"),
        POIDS_CORPS("poids_corps.csv"),
    }

    class Fichier(val entete: List<String>, val lignes: List<Map<String, String>>) {
        val table: Table?
            get() = when {
                "exercice_id" in entete -> Table.PERFORMANCES
                "nom" in entete && "type" in entete -> Table.EXERCICES
                "poids_kg" in entete -> Table.POIDS_CORPS
                else -> null
            }
    }

    fun lire(texte: String): Fichier {
        val enregistrements = decouper(texte.removePrefix("﻿"))
            .filter { ligne -> ligne.any { it.isNotBlank() } }
        if (enregistrements.isEmpty()) return Fichier(emptyList(), emptyList())
        val entete = enregistrements.first().map { it.trim().lowercase(Locale.ROOT) }
        val lignes = enregistrements.drop(1).map { valeurs ->
            entete.indices.associate { i -> entete[i] to (valeurs.getOrNull(i)?.trim() ?: "") }
        }
        return Fichier(entete, lignes)
    }

    /** Découpe en enregistrements et champs, guillemets doubles gérés (RFC 4180). */
    private fun decouper(texte: String): List<List<String>> {
        val resultat = mutableListOf<List<String>>()
        var ligne = mutableListOf<String>()
        val champ = StringBuilder()
        var entreGuillemets = false
        var i = 0
        while (i < texte.length) {
            val c = texte[i]
            if (entreGuillemets) {
                if (c == '"') {
                    if (i + 1 < texte.length && texte[i + 1] == '"') {
                        champ.append('"'); i++
                    } else {
                        entreGuillemets = false
                    }
                } else {
                    champ.append(c)
                }
            } else {
                when (c) {
                    '"' -> entreGuillemets = true
                    ',' -> { ligne.add(champ.toString()); champ.setLength(0) }
                    '\r' -> {}
                    '\n' -> {
                        ligne.add(champ.toString()); champ.setLength(0)
                        resultat.add(ligne); ligne = mutableListOf()
                    }
                    else -> champ.append(c)
                }
            }
            i++
        }
        if (champ.isNotEmpty() || ligne.isNotEmpty()) {
            ligne.add(champ.toString())
            resultat.add(ligne)
        }
        return resultat
    }

    // ---- Conversion lignes -> entités ----

    private fun booleen(v: String?): Boolean =
        v != null && (v == "1" || v.equals("true", ignoreCase = true))

    fun versExercice(l: Map<String, String>) = Exercice(
        id = l.getValue("id").toLong(),
        nom = l.getValue("nom"),
        type = TypeExercice.valueOf(l.getValue("type").uppercase(Locale.ROOT)),
        archive = booleen(l["archive"]),
        ordre = l["ordre"]?.toIntOrNull() ?: 0,
        poidsBarreKg = l["poids_barre_kg"]?.toDoubleOrNull() ?: Exercice.POIDS_BARRE_DEFAUT,
    )

    fun versPerformance(l: Map<String, String>) = Performance(
        id = l.getValue("id").toLong(),
        exerciceId = l.getValue("exercice_id").toLong(),
        date = LocalDate.parse(l.getValue("date")),
        chargeKg = l.getValue("charge_kg").toDouble(),
        reps = l.getValue("reps").toInt(),
        creeLe = l["cree_le"]?.toLongOrNull() ?: 0L,
    )

    fun versPesee(l: Map<String, String>) = Pesee(
        id = l["id"]?.toLongOrNull() ?: 0L,
        date = LocalDate.parse(l.getValue("date")),
        poidsKg = l.getValue("poids_kg").toDouble(),
    )

    // ---- Écriture ----

    private fun echapper(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v

    private fun nombre(d: Double): String = d.toString()

    fun ecrireExercices(liste: List<Exercice>): String = buildString {
        append("id,nom,type,archive,ordre,poids_barre_kg\n")
        liste.forEach {
            append("${it.id},${echapper(it.nom)},${it.type.name},${if (it.archive) 1 else 0},${it.ordre},${nombre(it.poidsBarreKg)}\n")
        }
    }

    fun ecrirePerformances(liste: List<Performance>): String = buildString {
        append("id,exercice_id,date,charge_kg,reps,cree_le\n")
        liste.sortedBy { it.id }.forEach {
            append("${it.id},${it.exerciceId},${it.date},${nombre(it.chargeKg)},${it.reps},${it.creeLe}\n")
        }
    }

    fun ecrirePesees(liste: List<Pesee>): String = buildString {
        append("id,date,poids_kg\n")
        liste.forEach { append("${it.id},${it.date},${nombre(it.poidsKg)}\n") }
    }
}

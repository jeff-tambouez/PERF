package fr.jeff.perf.domain

import fr.jeff.perf.data.Exercice
import fr.jeff.perf.data.Performance
import fr.jeff.perf.data.Pesee
import fr.jeff.perf.data.TypeExercice
import java.time.LocalDate
import kotlin.math.floor

object Calculs {
    const val PDC_DEFAUT = 80.0
    const val FENETRE_JOURS = 90L
    const val PAS_BARRE = 2.5
    const val PAS_LEST = 0.5

    fun epley(chargeTotale: Double, reps: Int): Double = chargeTotale * (1 + reps / 30.0)

    /** Arrondi à l'inférieur au pas donné (tolère les erreurs d'arrondi des doubles). */
    fun arrondiInferieur(valeur: Double, pas: Double): Double = floor(valeur / pas + 1e-9) * pas

    /** Poids de corps à une date : dernière pesée à cette date, sinon la première pesée connue, sinon 80 kg. */
    fun pdcA(date: LocalDate, peseesTriees: List<Pesee>): Double {
        if (peseesTriees.isEmpty()) return PDC_DEFAUT
        return peseesTriees.lastOrNull { !it.date.isAfter(date) }?.poidsKg ?: peseesTriees.first().poidsKg
    }

    /**
     * 1RM estimé (Epley) d'une saisie, ou null si non applicable (reps < 2, ou barre sans charge).
     * Poids du corps : résultat exprimé en lest.
     */
    fun estimation1RM(p: Performance, type: TypeExercice, pdc: Double): Double? {
        if (p.reps < 2) return null
        return when (type) {
            TypeExercice.BARRE -> if (p.chargeKg > 0) epley(p.chargeKg, p.reps) else null
            TypeExercice.POIDS_DU_CORPS -> epley(pdc + p.chargeKg, p.reps) - pdc
        }
    }

    /** Poids pour 5 reps à partir d'un 1RM (inverse d'Epley), arrondi à l'inférieur. Lest ≤ 0 → 0 (sans lest). */
    fun poids5Reps(rm1: Double, type: TypeExercice, pdc: Double): Double = when (type) {
        TypeExercice.BARRE -> arrondiInferieur(rm1 * 30.0 / 35.0, PAS_BARRE)
        TypeExercice.POIDS_DU_CORPS ->
            arrondiInferieur((pdc + rm1) * 30.0 / 35.0 - pdc, PAS_LEST).coerceAtLeast(0.0)
    }

    /**
     * Une valeur d'en-tête : réelle (avec sa date) ou estimée.
     * [reel] : pour une estimation retenue parce que plus haute, la meilleure valeur réelle de la période.
     */
    data class Valeur(val kg: Double, val estimee: Boolean, val date: LocalDate?, val reel: Valeur? = null)

    data class Entete(
        val rm1: Valeur?,
        val rm5: Valeur?,
        /** Vrai si aucune saisie dans les 90 derniers jours : valeurs calculées sur les 90 jours précédant la dernière saisie. */
        val ancien: Boolean,
    )

    /**
     * En-tête de la fiche exercice : 1RM et poids pour 5 reps, sur les 90 derniers jours.
     * La valeur réelle est retenue, sauf si l'estimation est plus haute
     * (ex. 1RM réel de 60 kg mais 75 kg x 6 réalisés) : on affiche alors l'estimation, marquée « estimé ».
     */
    fun entete(
        exercice: Exercice,
        performances: List<Performance>,
        peseesTriees: List<Pesee>,
        aujourdhui: LocalDate,
    ): Entete {
        if (performances.isEmpty()) return Entete(null, null, ancien = false)
        val derniere = performances.maxOf { it.date }
        val debutRecent = aujourdhui.minusDays(FENETRE_JOURS)
        val ancien = derniere.isBefore(debutRecent)
        val ancre = if (ancien) derniere else aujourdhui
        val debut = ancre.minusDays(FENETRE_JOURS)
        val fenetre = performances.filter { !it.date.isBefore(debut) && !it.date.isAfter(ancre) }

        val barre = exercice.type == TypeExercice.BARRE
        val avecChargeValide: (Performance) -> Boolean = { !barre || it.chargeKg > 0 }

        val reel1 = fenetre
            .filter { it.reps == 1 && avecChargeValide(it) }
            .maxWithOrNull(compareBy<Performance>({ it.chargeKg }, { it.date }))
            ?.let { Valeur(it.chargeKg, estimee = false, date = it.date) }

        val estime1 = fenetre
            .mapNotNull { p -> estimation1RM(p, exercice.type, pdcA(p.date, peseesTriees)) }
            .maxOrNull()
            ?.let { Valeur(arrondiInferieur(it, PAS_LEST), estimee = true, date = null) }

        val rm1 = meilleure(reel1, estime1)

        val reel5 = fenetre
            .filter { it.reps >= 5 && avecChargeValide(it) }
            .maxWithOrNull(compareBy<Performance>({ it.chargeKg }, { it.date }))
            ?.let { Valeur(it.chargeKg, estimee = false, date = it.date) }

        val estime5 = rm1?.let {
            Valeur(poids5Reps(it.kg, exercice.type, pdcA(ancre, peseesTriees)), estimee = true, date = null)
        }

        return Entete(rm1, meilleure(reel5, estime5), ancien)
    }

    private fun meilleure(reelle: Valeur?, estimee: Valeur?): Valeur? = when {
        reelle == null -> estimee
        estimee == null -> reelle
        estimee.kg > reelle.kg -> estimee.copy(reel = reelle)
        else -> reelle
    }

    // ---- Séries pour les courbes ----

    data class Point(val date: LocalDate, val valeur: Double)

    /** 1RM réel par jour (charge max à 1 rep). */
    fun serie1RMReel(exercice: Exercice, perfs: List<Performance>): List<Point> =
        perfs.filter { it.reps == 1 && (exercice.type != TypeExercice.BARRE || it.chargeKg > 0) }
            .groupBy { it.date }
            .map { (d, l) -> Point(d, l.maxOf { it.chargeKg }) }
            .sortedBy { it.date }

    /** 1RM estimé par jour (max des estimations du jour). */
    fun serie1RMEstime(exercice: Exercice, perfs: List<Performance>, peseesTriees: List<Pesee>): List<Point> =
        perfs.groupBy { it.date }
            .mapNotNull { (d, l) ->
                l.mapNotNull { estimation1RM(it, exercice.type, pdcA(d, peseesTriees)) }.maxOrNull()?.let { Point(d, it) }
            }
            .sortedBy { it.date }

    /** Reps max par jour à une charge donnée (0 = sans charge). */
    fun serieReps(perfs: List<Performance>, charge: Double): List<Point> =
        perfs.filter { it.chargeKg == charge }
            .groupBy { it.date }
            .map { (d, l) -> Point(d, l.maxOf { it.reps }.toDouble()) }
            .sortedBy { it.date }

    /** Moyenne glissante sur 7 jours calendaires (pesées comprises entre J-6 et J). */
    fun moyenneGlissante(peseesTriees: List<Pesee>, jours: Long = 7): List<Point> =
        peseesTriees.map { p ->
            val debut = p.date.minusDays(jours - 1)
            val fenetre = peseesTriees.filter { !it.date.isBefore(debut) && !it.date.isAfter(p.date) }
            Point(p.date, fenetre.map { it.poidsKg }.average())
        }
}

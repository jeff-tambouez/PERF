package fr.jeff.perf.domain

import fr.jeff.perf.data.Exercice
import fr.jeff.perf.data.TypeExercice
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Format {
    private val fr = Locale.FRANCE
    private val dateCourte = DateTimeFormatter.ofPattern("d MMM", fr)
    private val dateMoyenne = DateTimeFormatter.ofPattern("d MMM yyyy", fr)
    private val dateLongue = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", fr)

    /** 80 -> "80", 72.5 -> "72,5", 81.24 -> "81,2". */
    fun nombre(x: Double): String {
        val dixiemes = (x * 10).roundToLong()
        return if (dixiemes % 10 == 0L) (dixiemes / 10).toString()
        else String.format(fr, "%.1f", dixiemes / 10.0)
    }

    fun enKg(x: Double): String = "${nombre(x)} kg"

    fun ecart(x: Double): String = (if (x > 0) "+" else if (x < 0) "−" else "") + "${nombre(abs(x))} kg"

    fun reps(n: Int): String = if (n <= 1) "$n rep" else "$n reps"

    /** Charge complète : « 20 + 65 = 85 kg » (barre), « +12,5 kg » ou « Sans lest » (poids du corps). */
    fun charge(exercice: Exercice, kg: Double): String = when (exercice.type) {
        TypeExercice.BARRE -> when {
            kg <= 0.0 -> "Sans charge"
            kg < exercice.poidsBarreKg -> enKg(kg)
            else -> "${nombre(exercice.poidsBarreKg)} + ${nombre(kg - exercice.poidsBarreKg)} = ${enKg(kg)}"
        }
        TypeExercice.POIDS_DU_CORPS -> if (kg <= 0.0) "Sans lest" else "+${enKg(kg)}"
    }

    /** Valeur principale de l'en-tête : « 85 kg », « +16 kg », « Sans lest ». */
    fun chargeCourte(exercice: Exercice, kg: Double): String = when (exercice.type) {
        TypeExercice.BARRE -> enKg(kg)
        TypeExercice.POIDS_DU_CORPS -> if (kg <= 0.0) "Sans lest" else "+${enKg(kg)}"
    }

    /** Détail barre + disques pour l'en-tête : « 20 + 65 », ou null hors barre. */
    fun detailBarre(exercice: Exercice, kg: Double): String? =
        if (exercice.type == TypeExercice.BARRE && kg >= exercice.poidsBarreKg)
            "${nombre(exercice.poidsBarreKg)} + ${nombre(kg - exercice.poidsBarreKg)}"
        else null

    fun date(d: LocalDate): String = dateCourte.format(d)
    fun dateAvecAnnee(d: LocalDate): String = dateMoyenne.format(d)
    fun dateLongue(d: LocalDate): String = dateLongue.format(d).replaceFirstChar { it.titlecase(fr) }

    fun parseNombre(texte: String): Double? = texte.trim().replace(',', '.').toDoubleOrNull()
}

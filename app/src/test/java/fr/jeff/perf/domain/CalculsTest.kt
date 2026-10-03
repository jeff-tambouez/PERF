package fr.jeff.perf.domain

import fr.jeff.perf.data.Exercice
import fr.jeff.perf.data.Performance
import fr.jeff.perf.data.Pesee
import fr.jeff.perf.data.TypeExercice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalculsTest {
    private val squat = Exercice(id = 2, nom = "Squat", type = TypeExercice.BARRE)
    private val traction = Exercice(id = 3, nom = "Traction", type = TypeExercice.POIDS_DU_CORPS)
    private val jour = LocalDate.of(2026, 10, 2)

    private fun perf(ex: Exercice, charge: Double, reps: Int, date: LocalDate = jour) =
        Performance(exerciceId = ex.id, date = date, chargeKg = charge, reps = reps)

    @Test
    fun cinqRepsBarreArrondiA2_5() {
        assertEquals(85.0, Calculs.poids5Reps(100.0, TypeExercice.BARRE, 80.0), 1e-9)
    }

    @Test
    fun cinqRepsLestArrondiA0_5() {
        // (80 + 26,67) x 30/35 - 80 = 11,43 -> 11
        val rm1 = Calculs.epley(80.0, 10) - 80.0
        assertEquals(11.0, Calculs.poids5Reps(rm1, TypeExercice.POIDS_DU_CORPS, 80.0), 1e-9)
    }

    @Test
    fun cinqRepsLestNegatifDonneSansLest() {
        assertEquals(0.0, Calculs.poids5Reps(0.0, TypeExercice.POIDS_DU_CORPS, 80.0), 1e-9)
    }

    @Test
    fun enteteReelPrioritaire() {
        val e = Calculs.entete(squat, listOf(perf(squat, 100.0, 1)), emptyList(), jour)
        assertEquals(100.0, e.rm1!!.kg, 1e-9)
        assertFalse(e.rm1!!.estimee)
        assertEquals(85.0, e.rm5!!.kg, 1e-9)
        assertTrue(e.rm5!!.estimee)
    }

    @Test
    fun estimationPlusHauteQueLeReel() {
        // 60 x 1 et 75 x 6 : estimation 90 > 60
        val e = Calculs.entete(squat, listOf(perf(squat, 60.0, 1), perf(squat, 75.0, 6)), emptyList(), jour)
        assertEquals(90.0, e.rm1!!.kg, 1e-9)
        assertTrue(e.rm1!!.estimee)
        // 5 reps : réel 75 (75 x 6) contre estimé 90 x 30/35 = 77,1 -> 75 ; égalité -> réel
        assertEquals(75.0, e.rm5!!.kg, 1e-9)
        assertFalse(e.rm5!!.estimee)
    }

    @Test
    fun fenetre90Jours() {
        val vieux = perf(squat, 150.0, 1, jour.minusDays(120))
        val recent = perf(squat, 100.0, 1, jour.minusDays(10))
        val e = Calculs.entete(squat, listOf(vieux, recent), emptyList(), jour)
        assertEquals(100.0, e.rm1!!.kg, 1e-9)
        assertFalse(e.ancien)
    }

    @Test
    fun sansSaisieRecenteOnGardeLaDerniere() {
        val vieux = perf(squat, 150.0, 1, jour.minusDays(120))
        val e = Calculs.entete(squat, listOf(vieux), emptyList(), jour)
        assertEquals(150.0, e.rm1!!.kg, 1e-9)
        assertTrue(e.ancien)
    }

    @Test
    fun pdcALaDateDeLaSaisie() {
        val pesees = listOf(
            Pesee(date = jour.minusDays(30), poidsKg = 82.0),
            Pesee(date = jour.minusDays(5), poidsKg = 78.0),
        )
        assertEquals(82.0, Calculs.pdcA(jour.minusDays(40), pesees), 1e-9)
        assertEquals(82.0, Calculs.pdcA(jour.minusDays(10), pesees), 1e-9)
        assertEquals(78.0, Calculs.pdcA(jour, pesees), 1e-9)
        assertEquals(80.0, Calculs.pdcA(jour, emptyList()), 1e-9)
    }

    @Test
    fun enteteVide() {
        val e = Calculs.entete(traction, emptyList(), emptyList(), jour)
        assertNull(e.rm1)
        assertNull(e.rm5)
    }

    @Test
    fun formatBarre() {
        assertEquals("20 + 65 = 85 kg", Format.charge(squat, 85.0))
        assertEquals("20 + 52,5 = 72,5 kg", Format.charge(squat, 72.5))
        assertEquals("Sans lest", Format.charge(traction, 0.0))
        assertEquals("+12,5 kg", Format.charge(traction, 12.5))
    }
}

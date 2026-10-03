package fr.jeff.perf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.jeff.perf.domain.Calculs
import fr.jeff.perf.domain.Format
import java.time.LocalDate
import kotlin.math.abs

/** Une série tracée sur la courbe. */
data class Serie(
    val nom: String,
    val points: List<Calculs.Point>,
    val couleur: Color,
    val pointille: Boolean = false,
    val ligne: Boolean = true,
    val pointsVisibles: Boolean = true,
    val epaisseur: Dp = 2.dp,
)

/** Passage des valeurs (date, valeur) aux pixels. */
private class Repere(
    val jourMin: Float,
    val jourMax: Float,
    val vMin: Float,
    val vMax: Float,
    val gauche: Float,
    val haut: Float,
    val largeur: Float,
    val hauteur: Float,
) {
    fun x(jour: Long): Float = gauche + (jour.toFloat() - jourMin) / (jourMax - jourMin) * largeur
    fun y(v: Double): Float = haut + (1f - (v.toFloat() - vMin) / (vMax - vMin)) * hauteur
    fun position(p: Calculs.Point) = Offset(x(p.date.toEpochDay()), y(p.valeur))
}

/**
 * Courbe dessinée en Canvas, sans bibliothèque externe.
 * Un point par jour où une valeur existe, points reliés par une ligne.
 * Un appui sur un point affiche sa date et sa valeur.
 */
@Composable
fun Courbe(
    series: List<Serie>,
    formatValeur: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val tous = series.flatMap { it.points }
    val couleurTexte = MaterialTheme.colorScheme.onSurfaceVariant
    if (tous.isEmpty()) {
        Box(
            modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Pas de donnée sur la période", style = MaterialTheme.typography.bodyMedium, color = couleurTexte)
        }
        return
    }

    val mesureur = rememberTextMeasurer()
    val styleAxe = MaterialTheme.typography.labelSmall.copy(color = couleurTexte)
    val couleurGrille = MaterialTheme.colorScheme.outlineVariant
    var selection by remember(series) { mutableStateOf<Pair<Serie, Calculs.Point>?>(null) }

    val premierJour = tous.minOf { it.date.toEpochDay() }
    val dernierJour = tous.maxOf { it.date.toEpochDay() }
    val jourMin = if (premierJour == dernierJour) premierJour - 1f else premierJour.toFloat()
    val jourMax = if (premierJour == dernierJour) dernierJour + 1f else dernierJour.toFloat()
    val valMin = tous.minOf { it.valeur }
    val valMax = tous.maxOf { it.valeur }
    val marge = if (valMax - valMin < 1e-9) maxOf(1.0, abs(valMax) * 0.1) else (valMax - valMin) * 0.15
    val vMin = (valMin - marge).toFloat()
    val vMax = (valMax + marge).toFloat()

    Column(modifier) {
        Text(
            text = selection?.let { (s, p) -> "${Format.dateAvecAnnee(p.date)} : ${formatValeur(p.valeur)} (${s.nom})" }
                ?: "Touchez un point pour voir sa valeur",
            style = MaterialTheme.typography.labelMedium,
            color = if (selection != null) MaterialTheme.colorScheme.onSurface else couleurTexte,
        )
        Spacer(Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(series) {
                    detectTapGestures { appui ->
                        val r = Repere(
                            jourMin, jourMax, vMin, vMax,
                            gauche = 44.dp.toPx(), haut = 8.dp.toPx(),
                            largeur = size.width - 52.dp.toPx(), hauteur = size.height - 32.dp.toPx(),
                        )
                        selection = series
                            .flatMap { s -> s.points.map { s to it } }
                            .minByOrNull { (_, p) ->
                                val d = r.position(p) - appui
                                d.x * d.x + d.y * d.y
                            }
                    }
                },
        ) {
            val r = Repere(
                jourMin, jourMax, vMin, vMax,
                gauche = 44.dp.toPx(), haut = 8.dp.toPx(),
                largeur = size.width - 52.dp.toPx(), hauteur = size.height - 32.dp.toPx(),
            )

            // Grille horizontale et valeurs de l'axe Y
            listOf(valMin, (valMin + valMax) / 2, valMax).distinct().forEach { v ->
                val y = r.y(v)
                drawLine(couleurGrille, Offset(r.gauche, y), Offset(r.gauche + r.largeur, y), strokeWidth = 1.dp.toPx())
                val t = mesureur.measure(formatValeur(v), styleAxe)
                drawText(t, topLeft = Offset(r.gauche - t.size.width - 6.dp.toPx(), y - t.size.height / 2f))
            }

            // Dates de début et de fin sur l'axe X
            val yDates = r.haut + r.hauteur + 6.dp.toPx()
            val tDebut = mesureur.measure(Format.date(LocalDate.ofEpochDay(premierJour)), styleAxe)
            if (premierJour == dernierJour) {
                drawText(tDebut, topLeft = Offset(r.x(premierJour) - tDebut.size.width / 2f, yDates))
            } else {
                val tFin = mesureur.measure(Format.date(LocalDate.ofEpochDay(dernierJour)), styleAxe)
                drawText(tDebut, topLeft = Offset(r.gauche, yDates))
                drawText(tFin, topLeft = Offset(r.gauche + r.largeur - tFin.size.width, yDates))
            }

            // Séries
            series.forEach { s ->
                val positions = s.points.sortedBy { it.date }.map { r.position(it) }
                if (s.ligne && positions.size > 1) {
                    val chemin = Path().apply {
                        moveTo(positions.first().x, positions.first().y)
                        positions.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        chemin,
                        color = s.couleur,
                        style = Stroke(
                            width = s.epaisseur.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (s.pointille) PathEffect.dashPathEffect(floatArrayOf(14f, 12f)) else null,
                        ),
                    )
                }
                if (s.pointsVisibles) {
                    positions.forEach { drawCircle(s.couleur, radius = 3.5.dp.toPx(), center = it) }
                }
            }

            // Point sélectionné
            selection?.let { (s, p) ->
                val c = r.position(p)
                drawCircle(s.couleur, radius = 9.dp.toPx(), center = c, alpha = 0.2f)
                drawCircle(s.couleur, radius = 5.dp.toPx(), center = c)
            }
        }
    }
}

/** Légende : trait plein, pointillés ou points. */
@Composable
fun Legende(series: List<Serie>, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        series.forEach { s ->
            Canvas(Modifier.width(24.dp).height(10.dp)) {
                val y = size.height / 2
                if (s.ligne) {
                    drawLine(
                        s.couleur, Offset(0f, y), Offset(size.width, y),
                        strokeWidth = s.epaisseur.toPx(),
                        pathEffect = if (s.pointille) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null,
                    )
                } else {
                    drawCircle(s.couleur, radius = 3.5.dp.toPx(), center = Offset(size.width / 2, y))
                }
            }
            Text(
                s.nom,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, end = 16.dp),
            )
        }
    }
}

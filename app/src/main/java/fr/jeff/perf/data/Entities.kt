package fr.jeff.perf.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.LocalDate

enum class TypeExercice { BARRE, POIDS_DU_CORPS }

@Entity(
    tableName = "exercice",
    indices = [Index(value = ["nom"], unique = true)]
)
data class Exercice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val type: TypeExercice,
    val archive: Boolean = false,
    val ordre: Int = 0,
    @ColumnInfo(name = "poids_barre_kg") val poidsBarreKg: Double = POIDS_BARRE_DEFAUT,
) {
    companion object {
        const val POIDS_BARRE_DEFAUT = 20.0
    }
}

val Exercice.estBarre: Boolean get() = type == TypeExercice.BARRE

@Entity(
    tableName = "performance",
    indices = [Index(value = ["exercice_id", "date"])]
)
data class Performance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "exercice_id") val exerciceId: Long,
    val date: LocalDate,
    /** Barre : poids total (barre + disques). Poids du corps : lest seul. 0 = sans charge. */
    @ColumnInfo(name = "charge_kg") val chargeKg: Double,
    val reps: Int,
    @ColumnInfo(name = "cree_le") val creeLe: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "poids_corps",
    indices = [Index(value = ["date"], unique = true)]
)
data class Pesee(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    @ColumnInfo(name = "poids_kg") val poidsKg: Double,
)

class Converters {
    @TypeConverter
    fun dateVersTexte(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun texteVersDate(texte: String?): LocalDate? = texte?.let { LocalDate.parse(it) }

    @TypeConverter
    fun typeVersTexte(type: TypeExercice?): String? = type?.name

    @TypeConverter
    fun texteVersType(texte: String?): TypeExercice? = texte?.let { TypeExercice.valueOf(it) }
}

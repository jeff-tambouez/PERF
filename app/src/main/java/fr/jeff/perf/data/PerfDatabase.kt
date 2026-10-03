package fr.jeff.perf.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Exercice::class, Performance::class, Pesee::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PerfDatabase : RoomDatabase() {
    abstract fun exerciceDao(): ExerciceDao
    abstract fun performanceDao(): PerformanceDao
    abstract fun peseeDao(): PeseeDao

    companion object {
        fun creer(context: Context): PerfDatabase {
            val appContext = context.applicationContext
            return Room.databaseBuilder(appContext, PerfDatabase::class.java, "perf.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        Preremplissage.executer(appContext, db)
                    }
                })
                .build()
        }
    }
}

/**
 * Pré-remplissage à la création de la base, depuis assets/seed/*.csv
 * (exercices.csv, performances.csv et, s'il existe, poids_corps.csv).
 */
object Preremplissage {
    private const val TAG = "Preremplissage"

    fun executer(context: Context, db: SupportSQLiteDatabase) {
        val fichiers = try {
            context.assets.list("seed").orEmpty().filter { it.endsWith(".csv") }
        } catch (e: Exception) {
            Log.w(TAG, "Aucun dossier seed", e); emptyList()
        }
        db.beginTransaction()
        try {
            fichiers.forEach { nom ->
                val texte = context.assets.open("seed/$nom").bufferedReader(Charsets.UTF_8).use { it.readText() }
                val csv = Csv.lire(texte)
                when (csv.table) {
                    Csv.Table.EXERCICES -> csv.lignes.map(Csv::versExercice).forEach { e ->
                        db.execSQL(
                            "INSERT INTO exercice (id, nom, type, archive, ordre, poids_barre_kg) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(e.id, e.nom, e.type.name, if (e.archive) 1 else 0, e.ordre, e.poidsBarreKg)
                        )
                    }
                    Csv.Table.PERFORMANCES -> csv.lignes.map(Csv::versPerformance).forEach { p ->
                        db.execSQL(
                            "INSERT INTO performance (id, exercice_id, date, charge_kg, reps, cree_le) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(p.id, p.exerciceId, p.date.toString(), p.chargeKg, p.reps, p.creeLe)
                        )
                    }
                    Csv.Table.POIDS_CORPS -> csv.lignes.map(Csv::versPesee).forEach { p ->
                        db.execSQL(
                            "INSERT OR REPLACE INTO poids_corps (date, poids_kg) VALUES (?, ?)",
                            arrayOf<Any?>(p.date.toString(), p.poidsKg)
                        )
                    }
                    null -> Log.w(TAG, "En-tête non reconnu : $nom")
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Échec du pré-remplissage", e)
        } finally {
            db.endTransaction()
        }
    }
}

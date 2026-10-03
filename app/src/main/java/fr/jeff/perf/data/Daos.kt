package fr.jeff.perf.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciceDao {
    @Query("SELECT * FROM exercice ORDER BY ordre, nom")
    fun tous(): Flow<List<Exercice>>

    @Query("SELECT * FROM exercice ORDER BY ordre, nom")
    suspend fun tousMaintenant(): List<Exercice>

    @Query("SELECT * FROM exercice WHERE id = :id")
    fun parId(id: Long): Flow<Exercice?>

    @Query("SELECT COALESCE(MAX(ordre), -1) + 1 FROM exercice")
    suspend fun prochainOrdre(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserer(exercice: Exercice): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insererTous(exercices: List<Exercice>)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun modifier(exercice: Exercice)

    @Update
    suspend fun modifierTous(exercices: List<Exercice>)

    @Query("DELETE FROM exercice")
    suspend fun toutSupprimer()
}

@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performance ORDER BY date DESC, cree_le DESC")
    fun toutes(): Flow<List<Performance>>

    @Query("SELECT * FROM performance ORDER BY date DESC, cree_le DESC")
    suspend fun toutesMaintenant(): List<Performance>

    @Query("SELECT * FROM performance WHERE exercice_id = :exerciceId ORDER BY date DESC, cree_le DESC")
    fun parExercice(exerciceId: Long): Flow<List<Performance>>

    @Query("SELECT * FROM performance WHERE id = :id")
    suspend fun parId(id: Long): Performance?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrer(performance: Performance): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererTous(performances: List<Performance>)

    @Delete
    suspend fun supprimer(performance: Performance)

    @Query("DELETE FROM performance")
    suspend fun toutSupprimer()
}

@Dao
interface PeseeDao {
    @Query("SELECT * FROM poids_corps ORDER BY date")
    fun toutes(): Flow<List<Pesee>>

    @Query("SELECT * FROM poids_corps ORDER BY date")
    suspend fun toutesMaintenant(): List<Pesee>

    /** REPLACE : une pesée existante à la même date est remplacée (une pesée par jour). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrer(pesee: Pesee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererTous(pesees: List<Pesee>)

    @Delete
    suspend fun supprimer(pesee: Pesee)

    @Query("DELETE FROM poids_corps")
    suspend fun toutSupprimer()
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts WHERE userId = :userId ORDER BY startedAt DESC")
    fun getAttemptsForUser(userId: String): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE id = :attemptId AND userId = :userId LIMIT 1")
    fun getAttemptById(attemptId: String, userId: String): Flow<AttemptEntity?>

    @Query("SELECT * FROM attempts WHERE id = :attemptId AND userId = :userId LIMIT 1")
    suspend fun getAttemptByIdDirect(attemptId: String, userId: String): AttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Update
    suspend fun updateAttempt(attempt: AttemptEntity)

    @Query("DELETE FROM attempts WHERE id = :attemptId AND userId = :userId")
    suspend fun deleteAttempt(attemptId: String, userId: String): Int

    @Query("DELETE FROM attempts WHERE userId = :userId")
    suspend fun deleteAllAttemptsForUser(userId: String): Int
}

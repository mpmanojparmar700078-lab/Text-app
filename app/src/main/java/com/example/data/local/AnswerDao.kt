package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Query("""
        SELECT a.* FROM answers a 
        INNER JOIN attempts att ON a.attemptId = att.id 
        WHERE a.attemptId = :attemptId AND att.userId = :userId
    """)
    fun getAnswersForAttempt(attemptId: String, userId: String): Flow<List<AnswerEntity>>

    @Query("""
        SELECT a.* FROM answers a 
        INNER JOIN attempts att ON a.attemptId = att.id 
        WHERE a.attemptId = :attemptId AND att.userId = :userId
    """)
    suspend fun getAnswersForAttemptDirect(attemptId: String, userId: String): List<AnswerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<AnswerEntity>)

    @Query("DELETE FROM answers WHERE attemptId = :attemptId")
    suspend fun deleteAnswersForAttempt(attemptId: String): Int
}

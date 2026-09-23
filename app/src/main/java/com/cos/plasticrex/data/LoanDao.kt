package com.cos.plasticrex.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllLoans(): Flow<List<Loan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan)

    @Query("UPDATE loans SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM loans")
    suspend fun clearAll()
}
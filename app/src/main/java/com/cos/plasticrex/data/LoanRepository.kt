package com.cos.plasticrex.data

class LoanRepository(private val dao: LoanDao) {
    val allLoans = dao.getAllLoans()

    suspend fun insert(loan: Loan) = dao.insert(loan)
    suspend fun markCompleted(id: Long) = dao.markCompleted(id)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun clearAll() = dao.clearAll()
}
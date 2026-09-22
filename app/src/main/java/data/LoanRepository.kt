package com.yourname.plasticrex.data

class LoanRepository(private val dao: LoanDao) {
    val allLoans = dao.getAllLoans()

    suspend fun insert(loan: Loan) = dao.insert(loan)
    suspend fun clearAll() = dao.clearAll()
}
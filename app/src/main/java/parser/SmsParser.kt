package com.yourname.plasticrex.parser

import com.yourname.plasticrex.data.Loan
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object SmsParser {

    // ------------------------------------------------------------------
    // Amount patterns (covers most Indian formats)
    // ------------------------------------------------------------------
    private val amountPatterns = listOf(
        Pattern.compile("""(?:Rs\.?|INR|₹|rs)\s*([0-9,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""EMI\s*(?:of|amount|is|:)?\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9,]+(?:\.\d{1,2})?)\s*(?:Rs\.?|INR|₹)""", Pattern.CASE_INSENSITIVE)
    )

    // ------------------------------------------------------------------
    // Due date patterns
    // ------------------------------------------------------------------
    private val dueDatePatterns = listOf(
        Pattern.compile("""due\s+(?:on|date|by)?\s*:?\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:on|by)\s+(\d{1,2}(?:st|nd|rd|th)?\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})""", Pattern.CASE_INSENSITIVE)
    )

    // ------------------------------------------------------------------
    // Known Indian lenders / NBFCs (used for better naming)
    // ------------------------------------------------------------------
    private val knownLenders = listOf(
        "HDFC", "ICICI", "Axis", "SBI", "Kotak", "Yes Bank", "IndusInd",
        "Bajaj Finserv", "Bajaj", "KreditBee", "MoneyView", "LazyPay",
        "PaySense", "CASHe", "EarlySalary", "Home Credit", "TVS Credit",
        "Muthoot", "Manappuram", "Fullerton", "Tata Capital", "L&T Finance",
        "Mahindra Finance", "Shriram", "Indiabulls", "Poonawalla", "Navi",
        "Slice", "Uni", "Cred", "Amazon Pay Later", "Flipkart Pay Later",
        "Paytm Postpaid", "Simpl", "ZestMoney", "SmartCoin", "Ring",
        "Stashfin", "MoneyTap", "LoanTap", "Prefr", "Mpocket"
    )

    fun parse(message: String, sender: String): Loan? {
        val lower = message.lowercase()

        // Quick relevance filter
        val isRelevant = listOf(
            "emi", "loan", "repayment", "instalment", "installment",
            "due", "outstanding", "overdue", "collection", "nbfc"
        ).any { lower.contains(it) }

        if (!isRelevant) return null

        val amount = extractAmount(message) ?: return null
        val dueDate = extractDueDate(message) ?: System.currentTimeMillis()
        val lender = extractLender(sender, message)

        // Very basic remaining months (optional)
        val remaining = extractRemainingMonths(message)

        return Loan(
            lender = lender,
            emiAmount = amount,
            dueDate = dueDate,
            remainingMonths = remaining,
            rawMessage = message.take(250)
        )
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val raw = matcher.group(1)?.replace(",", "") ?: continue
                val value = raw.toDoubleOrNull()
                if (value != null && value in 100.0..500000.0) { // reasonable EMI range
                    return value
                }
            }
        }
        return null
    }

    private fun extractDueDate(text: String): Long? {
        val formats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy",
            "dd MMM yyyy", "dd MMMM yyyy", "d MMM yyyy", "d MMMM yyyy",
            "dd/MM", "dd-MM"
        )

        for (pattern in dueDatePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val dateStr = matcher.group(1)?.trim() ?: continue
                for (fmt in formats) {
                    try {
                        val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                        sdf.isLenient = false
                        val date = sdf.parse(dateStr) ?: continue

                        val cal = Calendar.getInstance()
                        cal.time = date

                        // If year is missing or very old, use current / next year
                        val year = cal.get(Calendar.YEAR)
                        if (year < 2000) {
                            val now = Calendar.getInstance()
                            cal.set(Calendar.YEAR, now.get(Calendar.YEAR))
                            // If the date has already passed this year, assume next year
                            if (cal.before(now)) {
                                cal.add(Calendar.YEAR, 1)
                            }
                        }
                        return cal.timeInMillis
                    } catch (_: Exception) {
                        // try next format
                    }
                }
            }
        }
        return null
    }

    private fun extractLender(sender: String, message: String): String {
        // 1. Try known lenders inside the message body
        for (lender in knownLenders) {
            if (message.contains(lender, ignoreCase = true)) {
                return lender
            }
        }

        // 2. Clean the SMS sender (common Indian format: VM-HDFCBK, AD-BAJAJF etc.)
        val clean = sender
            .replace(Regex("""^[A-Z]{2}-"""), "")          // remove VM- / AD-
            .replace(Regex("""[^A-Za-z0-9 ]"""), " ")
            .trim()
            .take(25)

        if (clean.length >= 3) return clean

        return "Unknown Lender"
    }

    private fun extractRemainingMonths(text: String): Int? {
        val patterns = listOf(
            Pattern.compile("""(\d+)\s*(?:remaining|left|pending)\s*(?:emi|instalment|installment|months?)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:emi|instalment|installment)\s*(\d+)\s*(?:of|/)\s*(\d+)""", Pattern.CASE_INSENSITIVE)
        )
        for (p in patterns) {
            val m = p.matcher(text)
            if (m.find()) {
                return m.group(1)?.toIntOrNull()
            }
        }
        return null
    }
}

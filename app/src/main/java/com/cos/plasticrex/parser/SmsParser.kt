package com.cos.plasticrex.parser

import com.cos.plasticrex.data.Loan
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

object SmsParser {

    // ------------------------------------------------------------------
    // Strong positive signals
    // ------------------------------------------------------------------
    private val strongLoanKeywords = listOf(
        "emi",
        "smartemi",
        "instalment",
        "installment",
        "loan emi",
        "emi due",
        "emi amount",
        "emi of",
        "your emi",
        "upcoming emi",
        "loan repayment",
        "repayment due",
        "emi reminder"
    )

    private val weakLoanKeywords = listOf(
        "loan",
        "nbfc",
        "outstanding",
        "overdue",
        "collection",
        "due date",
        "payment due"
    )

    // ------------------------------------------------------------------
    // Strong exclusion list
    // ------------------------------------------------------------------
    private val excludeKeywords = listOf(
        "recharge", "talktime", "data pack", "data plan", "validity",
        "prepaid", "postpaid bill", "mobile bill", "electricity bill",
        "gas bill", "water bill", "broadband", "dth", "ott",
        "plan activated", "plan renewed", "balance is", "main balance",
        "wallet", "cashback", "reward", "offer", "coupon",
        "order", "delivered", "shipped", "tracking",
        "otp", "one time password", "verification code",
        "transaction successful", "payment received", "credited to",
        "debited from", "a/c", "account xx", "upi", "imps", "neft",

        // Hindi / Marathi / mixed
        "रिचार्ज", "रिचार्ज करें", "प्लान", "प्लान समाप्त",
        "समाप्त होने वाला", "डेटा", "अनलिमिटेड", "कॉलिंग",
        "टॉकटाईम", "वैलिडिटी", "प्रीपेड", "पोस्टपेड",
        "मोबाइल बिल", "बिल",

        "jio", "airtel", "vi ", "vodafone", "bsnl",
        "jiohotstar", "jiotv", "jio cinema", "hotstar",
        "google gemini", "unlimited 5g", "per day", "gb डेटा",
        "नंबर", "आपका jio", "आपका airtel", "आपका vi"
    )

    private val telecomBrands = listOf(
        "jio", "airtel", "vi", "vodafone", "idea", "bsnl", "mtnl"
    )

    // ------------------------------------------------------------------
    // EMI amount patterns
    //
    // IMPORTANT:
    // EMI-specific patterns come FIRST so that:
    // "loan request of INR 25,000 ... first EMI of INR 2,250"
    // returns 2250 instead of 25000.
    // ------------------------------------------------------------------
    private val emiAmountPatterns = listOf(

        // "first EMI of INR 2,250.00"
        Pattern.compile(
            """(?:first\s+)?emi\s+(?:of|amount|is|:)?\s*(?:Rs\.?|INR|₹|rs)\s*([0-9,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        ),

        // "EMI INR 2,250"
        Pattern.compile(
            """\bemi\b\s*(?:Rs\.?|INR|₹|rs)\s*([0-9,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        ),

        // "EMI = 2250" / "EMI: 2250"
        Pattern.compile(
            """\bemi\b\s*(?:=|:|-)?\s*([0-9,]+(?:\.\d{1,2})?)\b""",
            Pattern.CASE_INSENSITIVE
        )
    )

    // Generic fallback amount patterns
    private val amountPatterns = listOf(
        Pattern.compile(
            """(?:Rs\.?|INR|₹|rs)\s*([0-9,]+(?:\.\d{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """([0-9,]+(?:\.\d{1,2})?)\s*(?:Rs\.?|INR|₹)""",
            Pattern.CASE_INSENSITIVE
        )
    )

    // ------------------------------------------------------------------
    // Due date patterns
    // ------------------------------------------------------------------
    private val dueDatePatterns = listOf(
        Pattern.compile(
            """due\s+(?:on|date|by)?\s*:?\s*(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """(?:on|by)\s+(\d{1,2}(?:st|nd|rd|th)?\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})""",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            """(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})""",
            Pattern.CASE_INSENSITIVE
        )
    )

    // ------------------------------------------------------------------
    // Known lenders
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

        val lower = message.lowercase(Locale.getDefault())

        // --------------------------------------------------------------
        // 1. Hard exclusion
        // --------------------------------------------------------------
        if (excludeKeywords.any { lower.contains(it) }) {
            return null
        }

        // --------------------------------------------------------------
        // 2. Telecom protection
        // --------------------------------------------------------------
        val hasTelecomBrand = telecomBrands.any { lower.contains(it) }

        val hasRechargeSignal = listOf(
            "plan",
            "प्लान",
            "recharge",
            "रिचार्ज",
            "data",
            "डेटा",
            "validity",
            "वैलिडिटी",
            "talktime",
            "टॉकटाईम",
            "unlimited",
            "अनलिमिटेड"
        ).any { lower.contains(it) }

        if (hasTelecomBrand && hasRechargeSignal) {
            return null
        }

        // --------------------------------------------------------------
        // 3. Loan/EMI relevance
        // --------------------------------------------------------------
        val hasStrong = strongLoanKeywords.any { lower.contains(it) }
        val hasWeak = weakLoanKeywords.any { lower.contains(it) }

        if (!hasStrong && !hasWeak) {
            return null
        }

        // --------------------------------------------------------------
        // 4. Extract EMI amount
        // --------------------------------------------------------------
        val amount = extractAmount(message) ?: return null

        // Reasonable EMI range
        if (amount < 300.0 || amount > 300000.0) {
            return null
        }

        // --------------------------------------------------------------
        // 5. Due date
        // --------------------------------------------------------------
        val dueDate = extractDueDate(message)

        // Weak messages need a clear due date
        if (!hasStrong && dueDate == null) {
            return null
        }

        val finalDueDate = dueDate ?: System.currentTimeMillis()

        // --------------------------------------------------------------
        // 6. Lender
        // --------------------------------------------------------------
        val lender = extractLender(sender, message)

        // --------------------------------------------------------------
        // 7. Tenure / remaining months
        // --------------------------------------------------------------
        val remaining = extractRemainingMonths(message)

        return Loan(
            lender = lender,
            emiAmount = amount,
            dueDate = finalDueDate,
            remainingMonths = remaining,
            rawMessage = message.take(250)
        )
    }

    // ------------------------------------------------------------------
    // Extract EMI amount
    // ------------------------------------------------------------------
    private fun extractAmount(text: String): Double? {

        // First: look for an explicitly labelled EMI amount
        for (pattern in emiAmountPatterns) {
            val matcher = pattern.matcher(text)

            if (matcher.find()) {
                val raw = matcher.group(1)
                    ?.replace(",", "")
                    ?: continue

                val value = raw.toDoubleOrNull()

                if (value != null && value in 100.0..500000.0) {
                    return value
                }
            }
        }

        // Fallback: generic amount
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)

            if (matcher.find()) {
                val raw = matcher.group(1)
                    ?.replace(",", "")
                    ?: continue

                val value = raw.toDoubleOrNull()

                if (value != null && value in 100.0..500000.0) {
                    return value
                }
            }
        }

        return null
    }

    // ------------------------------------------------------------------
    // Extract due date
    // ------------------------------------------------------------------
    private fun extractDueDate(text: String): Long? {

        val formats = listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "dd/MM/yy",
            "dd-MM-yy",
            "dd MMM yyyy",
            "dd MMMM yyyy",
            "d MMM yyyy",
            "d MMMM yyyy",
            "dd/MM",
            "dd-MM"
        )

        for (pattern in dueDatePatterns) {

            val matcher = pattern.matcher(text)

            if (matcher.find()) {

                val dateStr = matcher.group(1)?.trim()
                    ?: continue

                for (fmt in formats) {

                    try {

                        val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                        sdf.isLenient = false

                        val date = sdf.parse(dateStr)
                            ?: continue

                        val cal = Calendar.getInstance()
                        cal.time = date

                        val year = cal.get(Calendar.YEAR)

                        if (year < 2000) {

                            val now = Calendar.getInstance()

                            cal.set(
                                Calendar.YEAR,
                                now.get(Calendar.YEAR)
                            )

                            if (cal.before(now)) {
                                cal.add(Calendar.YEAR, 1)
                            }
                        }

                        return cal.timeInMillis

                    } catch (_: Exception) {
                        // Try next format
                    }
                }
            }
        }

        return null
    }

    // ------------------------------------------------------------------
    // Extract lender
    // ------------------------------------------------------------------
    private fun extractLender(
        sender: String,
        message: String
    ): String {

        // First look for known lenders in message body
        for (lender in knownLenders) {

            if (message.contains(lender, ignoreCase = true)) {
                return lender
            }
        }

        // Clean sender
        val clean = sender
            .replace(Regex("""^[A-Z]{2}-"""), "")
            .replace(Regex("""[^A-Za-z0-9 ]"""), " ")
            .trim()
            .take(25)

        if (clean.length >= 3) {
            return clean
        }

        return "Unknown Lender"
    }

    // ------------------------------------------------------------------
    // Extract tenure / remaining months
    // ------------------------------------------------------------------
    private fun extractRemainingMonths(text: String): Int? {

        val patterns = listOf(

            // "12 months remaining"
            Pattern.compile(
                """(\d+)\s*(?:remaining|left|pending)\s*(?:emi|instalment|installment|months?)""",
                Pattern.CASE_INSENSITIVE
            ),

            // "EMI 3 of 12"
            Pattern.compile(
                """(?:emi|instalment|installment)\s*(\d+)\s*(?:of|/)\s*(\d+)""",
                Pattern.CASE_INSENSITIVE
            ),

            // "Tenure: 12 months"
            Pattern.compile(
                """tenure\s*:?\s*(\d+)\s*months?""",
                Pattern.CASE_INSENSITIVE
            )
        )

        for (pattern in patterns) {

            val matcher = pattern.matcher(text)

            if (matcher.find()) {

                return when {
                    matcher.group(1) != null ->
                        matcher.group(1)?.toIntOrNull()

                    else -> null
                }
            }
        }

        return null
    }

    // ------------------------------------------------------------------
    // Deduplication helper
    // ------------------------------------------------------------------
    fun isSameLoan(a: Loan, b: Loan): Boolean {

        if (!a.lender.equals(b.lender, ignoreCase = true)) {
            return false
        }

        val amountDiff = abs(a.emiAmount - b.emiAmount)

        val amountClose =
            amountDiff <= 50.0 ||
                    amountDiff / maxOf(a.emiAmount, 1.0) <= 0.03

        if (!amountClose) {
            return false
        }

        val dayDiff =
            abs(a.dueDate - b.dueDate) /
                    (1000 * 60 * 60 * 24)

        return dayDiff <= 5
    }
}
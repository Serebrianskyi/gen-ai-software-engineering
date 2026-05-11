package com.ai.homework.service

import com.ai.homework.dto.ClassificationResult
import com.ai.homework.model.Ticket
import com.ai.homework.model.TicketCategory
import com.ai.homework.model.TicketPriority
import org.springframework.stereotype.Service

@Service
class ClassificationService {

    companion object {
        private val CATEGORY_KEYWORDS = mapOf(
            TicketCategory.ACCOUNT_ACCESS to listOf(
                "login", "password", "signin", "sign in", "authentication", "2fa",
                "two-factor", "access", "verify", "account locked", "reset password"
            ),
            TicketCategory.TECHNICAL_ISSUE to listOf(
                "error", "crash", "bug", "exception", "issue", "problem", "broken",
                "not working", "fail", "failed", "failure", "timeout", "timeout"
            ),
            TicketCategory.BILLING_QUESTION to listOf(
                "invoice", "payment", "charge", "billing", "refund", "subscription",
                "bill", "cost", "price", "fee", "credit", "transaction"
            ),
            TicketCategory.FEATURE_REQUEST to listOf(
                "feature", "enhancement", "improvement", "request", "suggest", "idea",
                "would like", "can you add", "ability", "capability"
            ),
            TicketCategory.BUG_REPORT to listOf(
                "bug", "defect", "reproduce", "steps to reproduce", "expected", "actual"
            )
        )

        private val PRIORITY_KEYWORDS = mapOf(
            TicketPriority.URGENT to listOf(
                "urgent", "critical", "emergency", "asap", "as soon as possible",
                "immediately", "production down", "can't access", "cannot access",
                "security", "breach", "attack"
            ),
            TicketPriority.HIGH to listOf(
                "important", "blocking", "urgent", "asap", "high", "high priority"
            ),
            TicketPriority.LOW to listOf(
                "minor", "cosmetic", "nice to have", "suggestion", "low priority"
            )
        )
    }

    fun classify(ticket: Ticket): ClassificationResult {
        val combinedText = "${ticket.subject} ${ticket.description}".lowercase()

        val categoryResult = classifyCategory(combinedText)
        val priorityResult = classifyPriority(combinedText)

        val allKeywords = categoryResult.second + priorityResult.second
        val totalMatches = allKeywords.size
        val confidence = if (totalMatches > 0) {
            minOf(1.0, totalMatches * 0.15)
        } else {
            0.0
        }

        return ClassificationResult(
            category = categoryResult.first,
            priority = priorityResult.first,
            confidence = confidence,
            keywordsFound = allKeywords.distinct(),
            reasoning = generateReasoning(categoryResult.first, priorityResult.first, allKeywords)
        )
    }

    private fun classifyCategory(text: String): Pair<TicketCategory, List<String>> {
        var bestCategory = TicketCategory.OTHER
        var bestMatch = 0
        val matchedKeywords = mutableListOf<String>()

        for ((category, keywords) in CATEGORY_KEYWORDS) {
            val matches = keywords.count { keyword -> text.contains(keyword) }
            if (matches > bestMatch) {
                bestMatch = matches
                bestCategory = category
                matchedKeywords.clear()
                matchedKeywords.addAll(keywords.filter { keyword -> text.contains(keyword) })
            }
        }

        return Pair(bestCategory, matchedKeywords)
    }

    private fun classifyPriority(text: String): Pair<TicketPriority, List<String>> {
        val matchedKeywords = mutableListOf<String>()

        for ((priority, keywords) in PRIORITY_KEYWORDS) {
            val matches = keywords.filter { keyword -> text.contains(keyword) }
            if (matches.isNotEmpty()) {
                matchedKeywords.addAll(matches)
                return Pair(priority, matchedKeywords)
            }
        }

        return Pair(TicketPriority.MEDIUM, matchedKeywords)
    }

    private fun generateReasoning(
        category: TicketCategory,
        priority: TicketPriority,
        keywords: List<String>
    ): String {
        return when {
            keywords.isEmpty() -> "No matching keywords found. Classified as $category with $priority priority."
            keywords.size < 3 -> "Classified based on ${keywords.size} keyword(s): ${keywords.distinct().joinToString(", ")}"
            else -> "Classified based on ${keywords.size} matching keywords"
        }
    }
}
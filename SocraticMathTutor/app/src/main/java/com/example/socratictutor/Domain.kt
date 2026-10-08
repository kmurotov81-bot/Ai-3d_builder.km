package com.example.socratictutor

enum class Mode { TUTOR, PRACTICE, TEST }

enum class Level { BEGINNER, INTERMEDIATE, ADVANCED }

enum class Difficulty(val key: String) {
    EASY("easy"), MEDIUM("medium"), HARD("hard");

    companion object {
        fun fromKey(s: String?, default: Difficulty = MEDIUM): Difficulty =
            entries.firstOrNull { it.key == s?.trim()?.lowercase() } ?: default
    }
}

enum class TutorStyle { GENTLE, CONCISE, CHALLENGING }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Topic keys are what the AI returns; they are also used for progress tracking. */
enum class Topic(val key: String) {
    ARITHMETIC("arithmetic"),
    FRACTIONS("fractions"),
    PERCENTAGES("percentages"),
    RATIOS("ratios"),
    LINEAR_EQUATIONS("linear_equations"),
    LINEAR_FUNCTIONS("linear_functions"),
    SYSTEMS("systems_of_equations"),
    INEQUALITIES("inequalities"),
    QUADRATICS("quadratics"),
    POLYNOMIALS("polynomials"),
    EXPONENTS("exponents"),
    RADICALS("radicals"),
    GEOMETRY("geometry"),
    COORDINATE_GEOMETRY("coordinate_geometry"),
    STATISTICS("statistics"),
    PROBABILITY("probability"),
    TRIGONOMETRY("trigonometry"),
    PRECALCULUS("precalculus"),
    CALCULUS("calculus"),
    WORD_PROBLEMS("word_problems"),
    SAT_MATH("sat_math"),
    UNKNOWN("unknown");

    companion object {
        /** Topics a student can choose in Practice/Test. */
        val selectable: List<Topic> = entries.filter { it != UNKNOWN }

        fun fromKey(s: String?): Topic {
            val k = s?.trim()?.lowercase()?.replace(' ', '_')?.replace('-', '_') ?: return UNKNOWN
            if (k.isEmpty()) return UNKNOWN
            return entries.firstOrNull {
                it.key == k || it.key.removeSuffix("s") == k.removeSuffix("s")
            } ?: UNKNOWN
        }
    }
}

enum class ReplyType(val key: String) {
    PROBLEM_CONFIRMATION("problem_confirmation"),
    STEP("step"),
    CORRECT("correct"),
    PARTIAL("partial"),
    INCORRECT("incorrect"),
    HINT("hint"),
    WHY("why"),
    MISTAKE_ANALYSIS("mistake_analysis"),
    GRAPH("graph"),
    FINAL("final"),
    PRACTICE("practice"),
    CLARIFICATION("clarification");

    companion object {
        fun fromKey(s: String?): ReplyType? {
            val k = s?.trim()?.lowercase() ?: return null
            if (k == "unclear" || k == "cannot_determine") return CLARIFICATION
            return entries.firstOrNull { it.key == k }
        }
    }
}

/** One turn sent to the AI (Claude, Gemini or the backend). */
data class ApiTurn(val role: String, val text: String, val imageB64: String? = null)

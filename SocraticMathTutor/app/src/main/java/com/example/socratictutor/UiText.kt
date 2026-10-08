package com.example.socratictutor

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun topicLabel(t: Topic): String = stringResource(
    when (t) {
        Topic.ARITHMETIC -> R.string.topic_arithmetic
        Topic.FRACTIONS -> R.string.topic_fractions
        Topic.PERCENTAGES -> R.string.topic_percentages
        Topic.RATIOS -> R.string.topic_ratios
        Topic.LINEAR_EQUATIONS -> R.string.topic_linear_equations
        Topic.LINEAR_FUNCTIONS -> R.string.topic_linear_functions
        Topic.SYSTEMS -> R.string.topic_systems
        Topic.INEQUALITIES -> R.string.topic_inequalities
        Topic.QUADRATICS -> R.string.topic_quadratics
        Topic.POLYNOMIALS -> R.string.topic_polynomials
        Topic.EXPONENTS -> R.string.topic_exponents
        Topic.RADICALS -> R.string.topic_radicals
        Topic.GEOMETRY -> R.string.topic_geometry
        Topic.COORDINATE_GEOMETRY -> R.string.topic_coordinate_geometry
        Topic.STATISTICS -> R.string.topic_statistics
        Topic.PROBABILITY -> R.string.topic_probability
        Topic.TRIGONOMETRY -> R.string.topic_trigonometry
        Topic.PRECALCULUS -> R.string.topic_precalculus
        Topic.CALCULUS -> R.string.topic_calculus
        Topic.WORD_PROBLEMS -> R.string.topic_word_problems
        Topic.SAT_MATH -> R.string.topic_sat_math
        Topic.UNKNOWN -> R.string.topic_unknown
    },
)

@Composable
fun difficultyLabel(d: Difficulty): String = stringResource(
    when (d) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)

@Composable
fun levelLabel(l: Level): String = stringResource(
    when (l) {
        Level.BEGINNER -> R.string.level_beginner
        Level.INTERMEDIATE -> R.string.level_intermediate
        Level.ADVANCED -> R.string.level_advanced
    },
)

@Composable
fun errorText(e: UiError): String = when (e.kind) {
    AiErrorKind.NOT_CONFIGURED -> stringResource(R.string.err_not_configured, e.detail ?: "API key")
    else -> stringResource(
        when (e.kind) {
            AiErrorKind.NO_INTERNET -> R.string.err_no_internet
            AiErrorKind.TIMEOUT -> R.string.err_timeout
            AiErrorKind.RATE_LIMIT -> R.string.err_rate_limit
            AiErrorKind.AUTH -> R.string.err_auth
            AiErrorKind.SERVER -> R.string.err_server
            AiErrorKind.EMPTY -> R.string.err_empty
            AiErrorKind.INVALID_RESPONSE -> R.string.err_invalid
            AiErrorKind.IMAGE_FAILED -> R.string.err_image
            AiErrorKind.CAMERA_UNAVAILABLE -> R.string.err_camera
            AiErrorKind.GRAPH_FAILED -> R.string.err_graph
            else -> R.string.err_unknown
        },
    )
}

@Composable
fun loadingText(k: LoadingKind): String = stringResource(
    when (k) {
        LoadingKind.READING_IMAGE -> R.string.loading_reading_image
        LoadingKind.READING_PROBLEM -> R.string.loading_reading_problem
        LoadingKind.CHECKING_ANSWER -> R.string.loading_checking_answer
        LoadingKind.PREPARING_HINT -> R.string.loading_hint
        LoadingKind.EXPLAINING_WHY -> R.string.loading_why
        LoadingKind.CHECKING_WORK -> R.string.loading_checking_work
        LoadingKind.GENERATING_PROBLEM -> R.string.loading_generating
    },
)

fun formatClock(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

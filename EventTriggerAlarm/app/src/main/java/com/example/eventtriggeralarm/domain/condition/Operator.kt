package com.example.eventtriggeralarm.domain.condition

/**
 * Comparison operator for structured leaf conditions (Battery, Weather, etc.).
 */
enum class Operator(val symbol: String) {
    EQUALS("=="),
    NOT_EQUALS("!="),
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<="),
    CONTAINS("contains"),
    NOT_CONTAINS("not contains")
}

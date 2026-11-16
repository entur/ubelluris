package org.entur.ror.ubelluris.utils

import java.time.DayOfWeek

private val WEEKDAYS = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val WEEKEND = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** The days of each NeTEx DayOfWeekEnumeration value: the single days, and the values Weekdays, Weekend, Everyday and None. */
private val DAYS_BY_NETEX_DAY_OF_WEEK: Map<String, List<DayOfWeek>> =
    DayOfWeek.entries.associate { day -> day.name.lowercase().replaceFirstChar { it.uppercase() } to listOf(day) } +
        mapOf(
            "Weekdays" to WEEKDAYS,
            "Weekend" to WEEKEND,
            "Everyday" to WEEKDAYS + WEEKEND,
            "None" to emptyList(),
        )

/**
 * Parses a NeTEx DaysOfWeek text, e.g. "Monday Tuesday" or "Weekdays". An empty text gives an empty list.
 * Fails on a value that is not in the NeTEx DayOfWeekEnumeration.
 */
fun parseNetexDaysOfWeek(daysOfWeekText: String): List<DayOfWeek> =
    daysOfWeekText
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .flatMap { netexDayOfWeek ->
            DAYS_BY_NETEX_DAY_OF_WEEK[netexDayOfWeek] ?: error("Unknown NeTEx DaysOfWeek value: $netexDayOfWeek")
        }.distinct()
        .sorted()

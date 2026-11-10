package org.entur.ror.ubelluris.utils

import java.time.DayOfWeek

/** Parses a NeTEx DaysOfWeek text, e.g. "Monday Tuesday". An empty text gives an empty list. */
fun parseNetexDaysOfWeek(daysOfWeekText: String): List<DayOfWeek> =
    daysOfWeekText
        .split(" ")
        .filter { it.isNotBlank() }
        .map { DayOfWeek.valueOf(it.uppercase()) }

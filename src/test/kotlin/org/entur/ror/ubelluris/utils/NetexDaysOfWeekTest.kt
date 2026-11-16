package org.entur.ror.ubelluris.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class NetexDaysOfWeekTest {
    private val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    private val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    @Test
    fun shouldParseSingleDays() {
        assertThat(parseNetexDaysOfWeek("Monday Sunday")).containsExactly(DayOfWeek.MONDAY, DayOfWeek.SUNDAY)
    }

    @Test
    fun shouldParseWeekdays() {
        assertThat(parseNetexDaysOfWeek("Weekdays")).containsExactlyElementsOf(weekdays)
    }

    @Test
    fun shouldParseWeekend() {
        assertThat(parseNetexDaysOfWeek("Weekend")).containsExactlyElementsOf(weekend)
    }

    @Test
    fun shouldParseEveryday() {
        assertThat(parseNetexDaysOfWeek("Everyday")).containsExactlyElementsOf(weekdays + weekend)
    }

    @Test
    fun shouldParseNoneAsNoDays() {
        assertThat(parseNetexDaysOfWeek("None")).isEmpty()
    }

    @Test
    fun shouldParseEmptyTextAsNoDays() {
        assertThat(parseNetexDaysOfWeek("")).isEmpty()
    }

    @Test
    fun shouldRemoveDuplicateDaysOfOverlappingValues() {
        assertThat(parseNetexDaysOfWeek("Weekend Sunday  Monday")).containsExactly(DayOfWeek.MONDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }

    @Test
    fun shouldFailOnUnknownValue() {
        assertThatThrownBy { parseNetexDaysOfWeek("Funday") }
            .hasMessageContaining("Unknown NeTEx DaysOfWeek value: Funday")
    }
}

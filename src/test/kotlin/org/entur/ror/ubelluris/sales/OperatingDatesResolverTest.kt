package org.entur.ror.ubelluris.sales

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.model.SalesSharedData
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class OperatingDatesResolverTest {
    // 2026-11-02 is a Monday. The period covers Monday 2026-11-02 to Sunday 2026-11-15, because ToDate is exclusive.
    private val calendar =
        SalesSharedData(
            dayTypes =
                mapOf(
                    "DayType:mondays" to listOf(DayOfWeek.MONDAY),
                    "DayType:mondaysWithChanges" to listOf(DayOfWeek.MONDAY),
                    "DayType:datesOnly" to emptyList(),
                ),
            operatingPeriods =
                mapOf("OperatingPeriod:1" to Pair(LocalDateTime.parse("2026-11-02T00:00:00"), LocalDateTime.parse("2026-11-16T00:00:00"))),
            dayTypeAssignments =
                mapOf(
                    "DayType:mondays" to "OperatingPeriod:1",
                    "DayType:mondaysWithChanges" to "OperatingPeriod:1",
                    "DayType:datesOnly" to "OperatingPeriod:1",
                ),
            dayTypeDateAssignments =
                mapOf(
                    "DayType:mondaysWithChanges" to
                        listOf(
                            DayTypeDateAssignment(LocalDate.parse("2026-11-09"), isAvailable = false),
                            DayTypeDateAssignment(LocalDate.parse("2026-11-11"), isAvailable = true),
                        ),
                    "DayType:datesOnly" to listOf(DayTypeDateAssignment(LocalDate.parse("2026-11-04"), isAvailable = true)),
                ),
        )

    private val resolver = OperatingDatesResolver(calendar)

    @Test
    fun shouldUseDaysOfWeekWithinOperatingPeriodWithExclusiveToDate() {
        assertThat(resolver.resolve(listOf("DayType:mondays"))).containsExactlyInAnyOrderElementsOf(dates("2026-11-02", "2026-11-09"))
    }

    @Test
    fun shouldApplyIncludedAndExcludedDates() {
        assertThat(
            resolver.resolve(listOf("DayType:mondaysWithChanges")),
        ).containsExactlyInAnyOrderElementsOf(dates("2026-11-02", "2026-11-11"))
    }

    @Test
    fun shouldUseOnlyIncludedDatesWhenDaysOfWeekIsEmpty() {
        assertThat(resolver.resolve(listOf("DayType:datesOnly"))).containsExactly(LocalDate.parse("2026-11-04"))
    }

    @Test
    fun shouldCombineDatesOfAllDayTypesAndIgnoreUnknownDayTypes() {
        assertThat(resolver.resolve(listOf("DayType:mondays", "DayType:datesOnly", "DayType:unknown")))
            .containsExactlyInAnyOrderElementsOf(dates("2026-11-02", "2026-11-04", "2026-11-09"))
    }

    private fun dates(vararg isoDates: String): List<LocalDate> = isoDates.map { LocalDate.parse(it) }
}

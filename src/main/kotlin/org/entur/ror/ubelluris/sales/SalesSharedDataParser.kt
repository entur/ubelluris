package org.entur.ror.ubelluris.sales

import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.model.SalesSharedData
import org.entur.ror.ubelluris.utils.NetexEntityCollector
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class SalesSharedDataParser {
    companion object {
        private const val DAY_TYPE = "DayType"
        private const val OPERATING_PERIOD = "OperatingPeriod"
        private const val DAY_TYPE_ASSIGNMENT = "DayTypeAssignment"
    }

    private val entityCollector = NetexEntityCollector(setOf(DAY_TYPE, OPERATING_PERIOD, DAY_TYPE_ASSIGNMENT))

    fun parse(sharedDataPath: Path): SalesSharedData {
        val entitiesByElementName = entityCollector.collect(sharedDataPath).groupBy { it.elementName }

        fun entitiesOf(elementName: String) = entitiesByElementName[elementName].orEmpty()

        val dayTypeAssignmentsByKind = entitiesOf(DAY_TYPE_ASSIGNMENT).partition { it.hasValue("OperatingPeriodRef") }

        return SalesSharedData(
            dayTypes =
                entitiesOf(DAY_TYPE).associate {
                    it.id to parseDaysOfWeek(it.valueOrNull("properties/PropertyOfDay/DaysOfWeek").orEmpty())
                },
            operatingPeriods =
                entitiesOf(OPERATING_PERIOD).associate {
                    it.id to Pair(LocalDateTime.parse(it.requireValue("FromDate")), LocalDateTime.parse(it.requireValue("ToDate")))
                },
            dayTypeAssignments =
                dayTypeAssignmentsByKind.first
                    .associate { it.requireValue("DayTypeRef") to it.requireValue("OperatingPeriodRef") },
            dayTypeDateAssignments =
                dayTypeAssignmentsByKind.second.groupBy(
                    keySelector = { it.requireValue("DayTypeRef") },
                    valueTransform = {
                        DayTypeDateAssignment(
                            date = LocalDate.parse(it.requireValue("Date")),
                            isAvailable = it.valueOrNull("isAvailable")?.toBooleanStrict() ?: true,
                        )
                    },
                ),
        )
    }

    private fun parseDaysOfWeek(daysOfWeekText: String): List<DayOfWeek> =
        daysOfWeekText
            .split(" ")
            .filter { it.isNotBlank() }
            .map { DayOfWeek.valueOf(it.uppercase()) }
}

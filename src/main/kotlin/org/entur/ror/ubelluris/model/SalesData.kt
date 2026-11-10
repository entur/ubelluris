package org.entur.ror.ubelluris.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

data class SalesData(
    val trainNrServiceJourneysMap: Map<String, List<SalesServiceJourney>>,
    val calendarData: SharedCalendarData,
) {
    companion object {
        val EMPTY =
            SalesData(
                trainNrServiceJourneysMap = emptyMap(),
                calendarData =
                    SharedCalendarData(
                        dayTypes = emptyMap(),
                        operatingPeriods = emptyMap(),
                        dayTypeAssignments = emptyMap(),
                        dayTypeDateAssignments = emptyMap(),
                    ),
            )
    }
}

// Populated from _shared_data
data class SharedCalendarData(
    val dayTypes: Map<String, List<DayOfWeek>>, // DayType id -> DaysOfWeek, empty when the DayType only uses dates
    val operatingPeriods: Map<String, Pair<LocalDateTime, LocalDateTime>>, // OperatingPeriod id -> FromDate, ToDate
    val dayTypeAssignments: Map<String, String>, // DayType id -> OperatingPeriod id
    val dayTypeDateAssignments: Map<String, List<DayTypeDateAssignment>>, // DayType id -> dates included or excluded
)

data class DayTypeDateAssignment(
    val date: LocalDate,
    val isAvailable: Boolean, // false: no service on this date, even when DaysOfWeek and OperatingPeriod include it
)

// Populated from line files
data class SalesServiceJourney(
    val serviceJourneyId: String,
    val productCode: String, // from keyValue list of service journey
    val salesAuthorityNumber: String, // from keyValue list of associated line
    val dayTypeRefs: List<String>,
    val trainNr: String,
)

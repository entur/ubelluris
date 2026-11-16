package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SalesMatchRepositoryTest {
    private val repository = SalesMatchRepository()

    @Test
    fun shouldFailOnDuplicateServiceJourneyId() {
        repository.addServiceJourney(serviceJourney())

        assertThatThrownBy { repository.addServiceJourney(serviceJourney()) }
            .hasMessageContaining("Duplicate ServiceJourney id: SJ:1")
    }

    @Test
    fun shouldFailOnOperatingPeriodWithoutToDate() {
        repository.addOperatingPeriodFromDate("OP:1", LocalDateTime.parse("2026-11-02T00:00:00"))

        assertThatThrownBy { repository.calendar() }
            .hasMessageContaining("OperatingPeriods without both FromDate and ToDate: [OP:1]")
    }

    @Test
    fun shouldFailOnOperatingPeriodWithoutFromDate() {
        repository.addOperatingPeriodToDate("OP:1", LocalDateTime.parse("2026-12-12T00:00:00"))

        assertThatThrownBy { repository.calendar() }
            .hasMessageContaining("OperatingPeriods without both FromDate and ToDate: [OP:1]")
    }

    @Test
    fun shouldFailOnUnresolvedLine() {
        repository.addServiceJourney(serviceJourney(transportMode = "rail"))

        assertThatThrownBy { repository.railServiceJourneys() }
            .hasMessageContaining("Unresolved Line for ServiceJourney SJ:1")
    }

    @Test
    fun shouldFailOnUnresolvedTrainNumberRef() {
        repository.addRouteRef("JP:1", "Route:1")
        repository.addLineRef("Route:1", "Line:1")
        repository.addLineTransportMode("Line:1", "rail")
        repository.addServiceJourney(serviceJourney(trainNumberRefs = listOf("TN:missing")))

        assertThatThrownBy { repository.railServiceJourneys() }
            .hasMessageContaining("Unresolved TrainNumberRefs in ServiceJourney SJ:1: [TN:missing]")
    }

    private fun serviceJourney(
        transportMode: String? = null,
        trainNumberRefs: List<String> = emptyList(),
    ) = CollectedServiceJourney(
        id = "SJ:1",
        journeyPatternRef = "JP:1",
        transportMode = transportMode,
        dayTypeRefs = listOf("DT:1"),
        trainNumberRefs = trainNumberRefs,
        keyListKeys = null,
        lastElementBeforeKeyList = null,
    )
}

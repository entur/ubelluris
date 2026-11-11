package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.model.DayTypeDateAssignment
import org.entur.ror.ubelluris.sales.TimetableRailServiceJourney
import org.entur.ror.ubelluris.sales.writeLineFile
import org.entur.ror.ubelluris.sales.writeTimetableSharedDataFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class SalesMatchDataCollectorTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var inputDir: Path
    private val repository = SalesMatchRepository()
    private val collector = SalesMatchDataCollector(repository)

    @BeforeEach
    fun createInputDir() {
        inputDir = Files.createDirectories(tempDir.resolve("input"))
        writeTimetableSharedDataFile(inputDir)
    }

    @Test
    fun shouldCollectCalendarFromSharedData() {
        collect()

        val calendar = repository.calendar()
        assertThat(calendar.dayTypes).containsExactlyInAnyOrderEntriesOf(
            mapOf("SE:100:DayType:mondays" to listOf(DayOfWeek.MONDAY), "SE:100:DayType:datesOnly" to emptyList()),
        )
        assertThat(calendar.operatingPeriods).containsExactlyEntriesOf(
            mapOf(
                "SE:100:OperatingPeriod:1" to
                    Pair(LocalDateTime.parse("2026-11-02T00:00:00"), LocalDateTime.parse("2026-11-16T00:00:00")),
            ),
        )
        assertThat(calendar.dayTypeAssignments).containsExactlyEntriesOf(mapOf("SE:100:DayType:mondays" to "SE:100:OperatingPeriod:1"))
        assertThat(calendar.dayTypeDateAssignments).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "SE:100:DayType:mondays" to listOf(DayTypeDateAssignment(LocalDate.parse("2026-11-09"), isAvailable = false)),
                "SE:100:DayType:datesOnly" to listOf(DayTypeDateAssignment(LocalDate.parse("2026-11-10"), isAvailable = true)),
            ),
        )
    }

    @Test
    fun shouldCollectRailServiceJourneysWithTrainNrsAndDayTypeRefs() {
        writeLineFile(
            inputDir,
            "line_rail.xml",
            lineId = "SE:100:Line:1",
            transportMode = "rail",
            trainNr = "100",
            serviceJourneyId = "SE:100:ServiceJourney:rail",
            dayTypeRefs = listOf("SE:100:DayType:mondays", "SE:100:DayType:datesOnly"),
        )

        collect()

        assertThat(repository.railServiceJourneys()).containsExactly(
            TimetableRailServiceJourney(
                serviceJourneyId = "SE:100:ServiceJourney:rail",
                trainNrs = listOf("100"),
                dayTypeRefs = listOf("SE:100:DayType:mondays", "SE:100:DayType:datesOnly"),
            ),
        )
        assertThat(repository.hasKeyList("SE:100:ServiceJourney:rail")).isTrue()
        assertThat(repository.keyListKeys("SE:100:ServiceJourney:rail")).containsExactlyInAnyOrder("productCode", "fingerprint")
        assertThat(repository.lastElementBeforeKeyList("SE:100:ServiceJourney:rail")).isNull()
    }

    @Test
    fun shouldLeaveOutBusesAndReplacementBusesOnRailLines() {
        writeLineFile(inputDir, "line_bus.xml", lineId = "SE:100:Line:1", transportMode = "bus", trainNr = null)
        writeLineFile(
            inputDir,
            "line_replacement_bus.xml",
            lineId = "SE:100:Line:2",
            transportMode = "rail",
            trainNr = null,
            serviceJourneyTransportMode = "bus",
            serviceJourneyId = "SE:100:ServiceJourney:replacement",
        )

        collect()

        assertThat(repository.railServiceJourneys()).isEmpty()
    }

    private fun collect() {
        val outputDir = Files.createDirectories(tempDir.resolve("output"))
        NetexProcessor(filterConfig = FilterConfigBuilder().withPlugins(listOf(collector)).build())
            .run(inputDir.toFile(), outputDir.toFile())
    }
}

package org.entur.ror.ubelluris.filter

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class TimetableFilterConfigTest {
    @TempDir
    lateinit var tempDir: Path

    private fun runFilter(resourceFileName: String): String {
        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                transportModes = listOf(TransportMode.TRAM),
            )
        val filterConfig = TimetableFilterConfig(cliConfig).build()

        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()

        val inputBytes =
            requireNotNull(javaClass.getResourceAsStream("/timetable/$resourceFileName")) {
                "Test resource not found: /timetable/$resourceFileName"
            }.readBytes()
        File(inputDir, resourceFileName).writeBytes(inputBytes)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        return outputFile.readText()
    }

    @Test
    fun shouldRemoveCountryRefFromTopographicPlace() {
        val result = runFilter("timetable-with-countryref.xml")

        // CountryRef should be removed
        assertThat(result).doesNotContain("<CountryRef")
        assertThat(result).doesNotContain("CountryRef ref=")

        // TopographicPlace should still exist
        assertThat(result).contains("<TopographicPlace")
        assertThat(result).contains("SE:050:TopographicPlace:AT")
        assertThat(result).contains("SE:050:TopographicPlace:DK")

        // Other TopographicPlace children should be preserved
        assertThat(result).contains("<IsoCode>AT</IsoCode>")
        assertThat(result).contains("<IsoCode>DK</IsoCode>")
        assertThat(result).contains("<Name>Austria</Name>")
        assertThat(result).contains("<Name>Denmark</Name>")
        assertThat(result).contains("<TopographicPlaceType>country</TopographicPlaceType>")
    }

    @Test
    fun shouldPreserveOtherTimetableData() {
        val result = runFilter("timetable-with-countryref.xml")

        // Verify timetable data is preserved
        assertThat(result).contains("<Route")
        assertThat(result).contains("SE:001:Route:R-tram")
        assertThat(result).contains("<Line")
        assertThat(result).contains("SE:001:Line:L-tram")
        assertThat(result).contains("<TransportMode>tram</TransportMode>")
        assertThat(result).contains("<JourneyPattern")
        assertThat(result).contains("<ScheduledStopPointRef")
    }

    @Test
    fun shouldHandleTimetableWithoutCountryRef() {
        val result = runFilter("sample-timetable.xml")

        // Should process without errors even when CountryRef is not present
        assertThat(result).contains("<Route")
        assertThat(result).contains("SE:001:Route:R-tram")
    }

    @Test
    fun shouldDeduplicateIdenticalServiceJourneyInterchanges() {
        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                transportModes = listOf(TransportMode.TRAM),
            )
        val timetableFilterConfig = TimetableFilterConfig(cliConfig)
        val filterConfig = timetableFilterConfig.build()

        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()

        val inputBytes =
            requireNotNull(javaClass.getResourceAsStream("/timetable/duplicate-service-journey-interchanges.xml")) {
                "Test resource not found: /timetable/duplicate-service-journey-interchanges.xml"
            }.readBytes()
        File(inputDir, "duplicate-service-journey-interchanges.xml").writeBytes(inputBytes)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        // Check what the plugin collected
        val collectedData = timetableFilterConfig.interchangeCollectorPlugin.getCollectedData()
        val identicalDuplicates = timetableFilterConfig.interchangeCollectorPlugin.getIdenticalDuplicates()
        val conflictingDuplicates = timetableFilterConfig.interchangeCollectorPlugin.getConflictingDuplicates()

        // Debug output
        println("Collected data: $collectedData")
        println("Identical duplicates: $identicalDuplicates")
        println("Conflicting duplicates: $conflictingDuplicates")

        assertThat(identicalDuplicates).containsExactly(
            "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001294_130000000000001168",
        )
        assertThat(conflictingDuplicates).containsExactly(
            "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001294_130000000000001169",
        )

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val result = outputFile.readText()

        // File contains:
        // - 1 unique ServiceJourneyInterchange (A_...1272_...1168)
        // - 2 identical duplicates of another (A_...1294_...1168)

        // The unique interchange should be preserved
        assertThat(result).contains("SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001272_130000000000001168")

        // The duplicate interchange ID should appear exactly once (first occurrence kept, second removed)
        val duplicateId = "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001294_130000000000001168"
        val count = result.split(duplicateId).size - 1
        assertThat(count)
            .withFailMessage(
                "Expected duplicate ServiceJourneyInterchange to appear exactly once, but found %d occurrences",
                count,
            ).isEqualTo(1)

        // Verify the total number of ServiceJourneyInterchange elements
        val totalInterchanges = result.split("<ServiceJourneyInterchange").size - 1
        assertThat(totalInterchanges)
            .withFailMessage("Expected 2 ServiceJourneyInterchange elements (1 unique + 1 deduplicated), but found %d", totalInterchanges)
            .isEqualTo(2)

        // Verify the content of the kept duplicate is correct
        assertThat(result).contains("<FromJourneyRef ref=\"SE:013:ServiceJourney:130000000000001294\"")
        assertThat(result).contains("<ToJourneyRef ref=\"SE:013:ServiceJourney:130000000000001168\"")
    }
}

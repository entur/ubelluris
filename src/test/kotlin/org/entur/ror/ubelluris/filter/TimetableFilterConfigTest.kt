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

        // Verify what the plugin collected during parsing
        val identicalDuplicates = timetableFilterConfig.interchangeCollectorPlugin.getIdenticalDuplicates()
        val conflictingDuplicates = timetableFilterConfig.interchangeCollectorPlugin.getConflictingDuplicates()

        assertThat(identicalDuplicates).containsExactly(
            "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001294_130000000000001168",
        )
        assertThat(conflictingDuplicates).containsExactly(
            "SE:013:ServiceJourneyInterchange:A_9022013003003001_130000000000001294_130000000000001169",
        )

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val actualOutput = outputFile.readText()

        // Load expected output
        val expectedOutput =
            requireNotNull(javaClass.getResourceAsStream("/timetable/deduplicated-service-journey-interchanges.xml")) {
                "Test resource not found: /timetable/deduplicated-service-journey-interchanges.xml"
            }.bufferedReader().use { it.readText() }

        // Normalize both outputs for comparison (ignore whitespace differences and dynamic timestamp)
        val normalizedActual = normalizeXml(actualOutput)
        val normalizedExpected = normalizeXml(expectedOutput)

        assertThat(normalizedActual).isEqualTo(normalizedExpected)
    }

    /**
     * Normalize XML for comparison by:
     * - Removing the PublicationTimestamp value (it's dynamic)
     * - Removing all whitespace between tags (to ignore indentation differences)
     */
    private fun normalizeXml(xml: String): String =
        xml
            .replace(Regex("<PublicationTimestamp>.*?</PublicationTimestamp>"), "<PublicationTimestamp/>")
            .replace(Regex(">\\s+<"), "><") // Remove whitespace between tags
            .trim()
}

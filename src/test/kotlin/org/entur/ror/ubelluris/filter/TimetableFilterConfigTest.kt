package org.entur.ror.ubelluris.filter

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.config.TimetableFilterFeatures
import org.entur.ror.ubelluris.emptySalesData
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class TimetableFilterConfigTest {
    @TempDir
    lateinit var tempDir: Path

    private fun runFilter(
        resourceFileName: String,
        timetableFilterFeatures: TimetableFilterFeatures = TimetableFilterFeatures(),
    ): String {
        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                transportModes = listOf(TransportMode.TRAM),
                timetableFilterFeatures = timetableFilterFeatures,
            )
        val filterConfig = TimetableFilterConfig(cliConfig, emptySalesData()).build()

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
    fun shouldRemoveRequestStop() {
        val result = runFilter("sample-timetable.xml")

        assertThat(result).doesNotContain("<RequestStop")
    }

    @Test
    fun shouldKeepCountryRefWhenFeatureDisabled() {
        val result =
            runFilter(
                "timetable-with-countryref.xml",
                TimetableFilterFeatures(skipTopographicPlaceCountryRef = false),
            )

        assertThat(result).contains("<CountryRef")
    }

    @Test
    fun shouldKeepRequestStopWhenFeatureDisabled() {
        val result = runFilter("sample-timetable.xml", TimetableFilterFeatures(skipRequestStop = false))

        assertThat(result).contains("<RequestStop")
    }

    @Test
    fun shouldNotRegisterDisabledFeatures() {
        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                timetableFilterFeatures =
                    TimetableFilterFeatures(
                        versionRefNormalizer = false,
                        transportModeToLocalScheduledStopPoint = false,
                        lineRemovalBasedOnPublicCodes = false,
                        lineRemovalBasedOnTransportMode = false,
                        skipTopographicPlaceCountryRef = false,
                        skipRequestStop = false,
                        publicationTimestamp = false,
                        bookWhenFilter = false,
                        timetabledPassingTimeId = false,
                        interchangeDeduplication = false,
                        firstAndLastStopBoardingAlighting = false,
                        emptyGroupOfStopPlacesMembersFilter = false,
                        lineOperatorEnricher = false,
                        pruneUnreferencedEntities = false,
                        useSalesData = false,
                    ),
            )

        val filterConfig = TimetableFilterConfig(cliConfig, emptySalesData()).build()

        assertThat(filterConfig.plugins).isEmpty()
        assertThat(filterConfig.entitySelectors).isEmpty()
        assertThat(filterConfig.skipElements).isEmpty()
        assertThat(filterConfig.customElementHandlers).isEmpty()
        assertThat(filterConfig.unreferencedEntitiesToPrune).isEmpty()
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
        val timetableFilterConfig = TimetableFilterConfig(cliConfig, emptySalesData())
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

    @Test
    fun shouldDisableAlightingOnFirstAndBoardingOnLastStopPointInJourneyPattern() {
        val result = runFilter("timetable-with-boarding-alighting.xml")

        assertThat(stopPoint(result, "JP-1-1"))
            .contains("<ForAlighting>false</ForAlighting>", "<ForBoarding>true</ForBoarding>")
        assertThat(stopPoint(result, "JP-1-2"))
            .contains("<ForAlighting>true</ForAlighting>", "<ForBoarding>true</ForBoarding>")
        assertThat(stopPoint(result, "JP-1-3"))
            .contains("<ForAlighting>true</ForAlighting>", "<ForBoarding>false</ForBoarding>")
    }

    @Test
    fun shouldDistinguishSameStopPointIdInDifferentJourneyPatterns() {
        val result = runFilter("timetable-with-boarding-alighting.xml")

        // JP-1-3 is last in JP-1 but first in JP-3
        val jp3 =
            requireNotNull(
                Regex(
                    "<JourneyPattern[^>]*id=\"SE:001:JourneyPattern:JP-3\".*?</JourneyPattern>",
                    RegexOption.DOT_MATCHES_ALL,
                ).find(result),
            ).value
        assertThat(stopPoint(jp3, "JP-1-3"))
            .contains("<ForAlighting>false</ForAlighting>", "<ForBoarding>true</ForBoarding>")
    }

    @Test
    fun shouldNotCorrectStopPointsThatOccurInMultipleFiles() {
        val inputDir = Files.createDirectories(tempDir.resolve("multi-input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("multi-output")).toFile()
        val input =
            requireNotNull(javaClass.getResourceAsStream("/timetable/timetable-with-boarding-alighting.xml")).readBytes()
        File(inputDir, "a.xml").writeBytes(input)
        File(inputDir, "b.xml").writeBytes(input)

        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                transportModes = listOf(TransportMode.TRAM),
            )
        NetexProcessor(filterConfig = TimetableFilterConfig(cliConfig, emptySalesData()).build()).run(inputDir, outputDir)

        listOf("a.xml", "b.xml").forEach { fileName ->
            val result = File(outputDir, fileName).readText()
            assertThat(stopPoint(result, "JP-1-1")).contains("<ForAlighting>true</ForAlighting>")
            assertThat(stopPoint(result, "JP-1-3")).contains("<ForBoarding>true</ForBoarding>")
        }
    }

    private fun stopPoint(
        xml: String,
        idSuffix: String,
    ): String =
        requireNotNull(
            Regex(
                "<StopPointInJourneyPattern[^>]*id=\"SE:001:StopPointInJourneyPattern:$idSuffix\".*?</StopPointInJourneyPattern>",
                RegexOption.DOT_MATCHES_ALL,
            ).find(xml),
        ) { "StopPointInJourneyPattern $idSuffix not found" }.value

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

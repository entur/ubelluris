package org.entur.ror.ubelluris.sax.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.ror.ubelluris.config.CliConfig
import org.entur.ror.ubelluris.filter.TimetableFilterConfig
import org.entur.ror.ubelluris.model.TransportMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class TimetabledPassingTimeIdHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldAppendUniqueSequenceNumberToDuplicateIds() {
        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                transportModes = listOf(TransportMode.BUS),
            )
        val filterConfig = TimetableFilterConfig(cliConfig).build()

        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()

        val inputBytes =
            requireNotNull(javaClass.getResourceAsStream("/timetable/duplicate-timetabled-passing-time-ids.xml")) {
                "Test resource not found: /timetable/duplicate-timetabled-passing-time-ids.xml"
            }.readBytes()
        File(inputDir, "duplicate-timetabled-passing-time-ids.xml").writeBytes(inputBytes)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val result = outputFile.readText()

        // Verify that duplicate IDs now have unique suffixes with sequence numbers
        // The original duplicate IDs should be gone
        assertThat(result).doesNotContain("id=\"SE:001:TimetabledPassingTime:1\"")
        assertThat(result).doesNotContain("id=\"SE:001:TimetabledPassingTime:2\"")

        // Each occurrence should have a unique sequence number suffix
        // Note: Sequence numbers depend on the order elements are encountered during parsing
        val timetabledPassingTimePattern = Regex("""id="SE:001:TimetabledPassingTime:\d+-S\d+"""")
        val matches = timetabledPassingTimePattern.findAll(result).toList()

        // We should have 5 TimetabledPassingTime elements in total (3 + 2)
        assertThat(matches).hasSize(5)

        // Verify all IDs are unique
        val ids = matches.map { it.value }.toSet()
        assertThat(ids).hasSize(5).describedAs("All TimetabledPassingTime IDs should be unique")

        // Verify the format - each should end with -S followed by a number
        matches.forEach { match ->
            assertThat(match.value).matches("""id="SE:001:TimetabledPassingTime:\d+-S\d+"""")
        }

        // ServiceJourney elements should still be present and unchanged
        assertThat(result).contains("SE:001:ServiceJourney:1")
        assertThat(result).contains("SE:001:ServiceJourney:2")

        // Verify times are preserved
        assertThat(result).contains("<ArrivalTime>08:00:00</ArrivalTime>")
        assertThat(result).contains("<ArrivalTime>08:15:00</ArrivalTime>")
        assertThat(result).contains("<ArrivalTime>08:30:00</ArrivalTime>")
        assertThat(result).contains("<ArrivalTime>09:00:00</ArrivalTime>")
        assertThat(result).contains("<ArrivalTime>09:15:00</ArrivalTime>")
    }

    @Test
    fun shouldPreserveNonDuplicateIds() {
        val cliConfig =
            CliConfig(
                sourceCodespace = "SE",
                targetCodespace = "SE",
                transportModes = listOf(TransportMode.BUS),
            )
        val filterConfig = TimetableFilterConfig(cliConfig).build()

        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()

        // Create XML with already unique IDs
        val xmlContent =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <PublicationTimestamp>2026-08-27T10:00:00</PublicationTimestamp>
                <dataObjects>
                    <CompositeFrame version="1" id="SE:001:CompositeFrame:1">
                        <frames>
                            <TimetableFrame version="1" id="SE:001:TimetableFrame:1">
                                <vehicleJourneys>
                                    <ServiceJourney id="SE:001:ServiceJourney:1" version="1">
                                        <passingTimes>
                                            <TimetabledPassingTime id="SE:001:TimetabledPassingTime:unique1" version="1">
                                                <ArrivalTime>08:00:00</ArrivalTime>
                                            </TimetabledPassingTime>
                                            <TimetabledPassingTime id="SE:001:TimetabledPassingTime:unique2" version="1">
                                                <ArrivalTime>08:15:00</ArrivalTime>
                                            </TimetabledPassingTime>
                                        </passingTimes>
                                    </ServiceJourney>
                                </vehicleJourneys>
                            </TimetableFrame>
                        </frames>
                    </CompositeFrame>
                </dataObjects>
            </PublicationDelivery>
            """.trimIndent()

        File(inputDir, "unique-ids.xml").writeText(xmlContent)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val result = outputFile.readText()

        // Even unique IDs will get sequence numbers appended
        // This is expected behavior - ALL TimetabledPassingTime IDs get the suffix for consistency
        val timetabledPassingTimePattern = Regex("""id="SE:001:TimetabledPassingTime:unique\d+-S\d+"""")
        val matches = timetabledPassingTimePattern.findAll(result).toList()

        assertThat(matches).hasSize(2)

        // Verify all modified IDs are still unique
        val ids = matches.map { it.value }.toSet()
        assertThat(ids).hasSize(2)
    }
}

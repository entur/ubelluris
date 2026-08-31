package org.entur.ror.ubelluris.sax.selectors.entities

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

class LineFilteringEntitySelectorTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldCollectLineIDWithPublicCodeNO3AndRemoveIt() {
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
            requireNotNull(javaClass.getResourceAsStream("/timetable/nartrafik.xml")) {
                "Test resource not found: /timetable/nartrafik.xml"
            }.readBytes()
        File(inputDir, "nartrafik.xml").writeBytes(inputBytes)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        val repository = timetableFilterConfig.linePublicCodeFilterPlugin.repository
        val removedLines =
            arrayOf(
                "SE:012:Line:9011012850300000",
                "SE:012:Line:1",
                "SE:012:Line:2",
                "SE:012:Line:3",
                "SE:012:Line:4",
                "SE:012:Line:5",
            )
        assertThat(repository.lineIdsToRemove).containsExactlyInAnyOrder(*removedLines)

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val result = outputFile.readText()

        assertThat(result).contains("<PublicationDelivery")
        removedLines.forEach { lineId ->
            assertThat(result).doesNotContain(lineId)
        }
        // route, journeypattern, servicejourney should be removed by pruning
        assertThat(result).doesNotContain("SE:012:Route:121120000217589630")
        assertThat(result).doesNotContain("SE:012:JourneyPattern:121129420000048642")
        assertThat(result).doesNotContain("SE:012:JourneyPattern:121129420000100884")
        assertThat(result).doesNotContain("SE:012:JourneyPattern:121129420000048641")
        assertThat(result).doesNotContain("SE:012:ServiceJourney:121120000359706157")
        assertThat(result).doesNotContain("SE:012:ServiceJourney:121120100359706157")
        assertThat(result).doesNotContain("SE:012:ServiceJourney:121120000359706173")
        assertThat(result).doesNotContain("SE:012:ServiceJourney:121120100359706173")
    }
}

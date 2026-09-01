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

class BookWhenFilterHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldReplaceAdvanceOnlyWithAdvanceAndDayOfTravel() {
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
            requireNotNull(javaClass.getResourceAsStream("/timetable/book-when.xml")) {
                "Test resource not found: /timetable/book-when.xml"
            }.readBytes()
        File(inputDir, "book-when.xml").writeBytes(inputBytes)

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        val outputFile = outputDir.listFiles()?.firstOrNull()
        requireNotNull(outputFile) { "No output file was created" }

        val result = outputFile.readText()

        // BookWhen value should be replaced from advanceOnly to advanceAndDayOfTravel
        assertThat(result).contains("<BookWhen>advanceAndDayOfTravel</BookWhen>")
        assertThat(result).doesNotContain("<BookWhen>advanceOnly</BookWhen>")

        // Other FlexibleServiceProperties should be preserved
        assertThat(result).contains("<FlexibleServiceType>fixedPassingTimes</FlexibleServiceType>")
        assertThat(result).contains("<BookingNote>Turen måste förbeställas på tel: 010-4761184, före 21.30 samma kväll.</BookingNote>")

        // ServiceJourney should still exist
        assertThat(result).contains("<ServiceJourney")
        assertThat(result).contains("SE:014:ServiceJourney:9011091029900000")
    }
}

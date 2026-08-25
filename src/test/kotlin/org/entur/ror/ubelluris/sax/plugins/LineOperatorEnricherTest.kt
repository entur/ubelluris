package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class LineOperatorEnricherTest {
    @TempDir
    lateinit var tempDir: Path

    private fun runEnricher(vararg resourceFileNames: String): LineOperatorEnricher {
        val plugin = LineOperatorEnricher()
        val filterConfig =
            FilterConfigBuilder()
                .withPlugins(listOf(plugin))
                .build()

        val inputDir = Files.createDirectories(tempDir.resolve("input")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output")).toFile()

        resourceFileNames.forEachIndexed { index, fileName ->
            val bytes =
                requireNotNull(javaClass.getResourceAsStream("/timetable/$fileName")) {
                    "Test resource not found: /timetable/$fileName"
                }.readBytes()
            File(inputDir, "${index}_$fileName").writeBytes(bytes)
        }

        NetexProcessor(
            filterConfig = filterConfig,
        ).run(inputDir, outputDir)

        return plugin
    }

    @Test
    fun shouldCollectSingleOperatorForLine() {
        val plugin = runEnricher("line-operator-single.xml")
        val collectedData = plugin.getCollectedData()

        // Line 1 has 3 ServiceJourneys, all with OP1
        assertThat(collectedData["TEST:Line:1"]).isNotNull
        assertThat(collectedData["TEST:Line:1"]).containsEntry("TEST:Operator:OP1", 3)
    }

    @Test
    fun shouldReturnCorrectOperatorForSingleOperatorLine() {
        val plugin = runEnricher("line-operator-single.xml")

        val operator = plugin.getMostCommonOperator("TEST:Line:1")
        assertThat(operator).isEqualTo("TEST:Operator:OP1")
    }

    @Test
    fun shouldCollectMultipleOperatorsForLine() {
        val plugin = runEnricher("line-operator-multiple.xml")
        val collectedData = plugin.getCollectedData()

        // Line 1 has 8 ServiceJourneys: 5 with OP1, 3 with OP2
        assertThat(collectedData["TEST:Line:1"]).isNotNull
        assertThat(collectedData["TEST:Line:1"]).containsEntry("TEST:Operator:OP1", 5)
        assertThat(collectedData["TEST:Line:1"]).containsEntry("TEST:Operator:OP2", 3)
    }

    @Test
    fun shouldReturnMostCommonOperatorWhenMultipleExist() {
        val plugin = runEnricher("line-operator-multiple.xml")

        val operator = plugin.getMostCommonOperator("TEST:Line:1")
        // OP1 has 5 occurrences, OP2 has 3, so OP1 should win
        assertThat(operator).isEqualTo("TEST:Operator:OP1")
    }

    @Test
    fun shouldReturnNullForLineWithoutOperatorData() {
        val plugin = runEnricher("line-operator-single.xml")

        val operator = plugin.getMostCommonOperator("TEST:Line:NONEXISTENT")
        assertThat(operator).isNull()
    }

    @Test
    fun shouldHandleLineWithExistingOperator() {
        val plugin = runEnricher("line-operator-multiple.xml")
        val collectedData = plugin.getCollectedData()

        // Line 2 has 1 ServiceJourney with OP3
        assertThat(collectedData["TEST:Line:2"]).isNotNull
        assertThat(collectedData["TEST:Line:2"]).containsEntry("TEST:Operator:OP3", 1)
    }

    @Test
    fun shouldAccumulateOperatorCountsAcrossMultipleFiles() {
        val plugin = runEnricher("line-operator-single.xml", "line-operator-single.xml")
        val collectedData = plugin.getCollectedData()

        // Processing the same file twice should double the counts
        assertThat(collectedData["TEST:Line:1"]).containsEntry("TEST:Operator:OP1", 6)
    }

    @Test
    fun shouldReturnAlphabeticallyFirstOperatorInCaseOfTie() {
        // Create a plugin and manually populate data with a tie
        val plugin = LineOperatorEnricher()
        val filterConfig =
            FilterConfigBuilder()
                .withPlugins(listOf(plugin))
                .build()

        // We need to create a test file with a perfect tie
        val tieXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex" version="1.0">
                <PublicationTimestamp>2026-08-24T10:00:00</PublicationTimestamp>
                <dataObjects>
                    <CompositeFrame id="TEST:CompositeFrame:1" version="1">
                        <frames>
                            <TimetableFrame id="TEST:TimetableFrame:1" version="1">
                                <vehicleJourneys>
                                    <ServiceJourney id="TEST:ServiceJourney:1" version="1">
                                        <LineRef ref="TEST:Line:TIE"/>
                                        <OperatorRef ref="TEST:Operator:OPZ"/>
                                    </ServiceJourney>
                                    <ServiceJourney id="TEST:ServiceJourney:2" version="1">
                                        <LineRef ref="TEST:Line:TIE"/>
                                        <OperatorRef ref="TEST:Operator:OPA"/>
                                    </ServiceJourney>
                                </vehicleJourneys>
                            </TimetableFrame>
                        </frames>
                    </CompositeFrame>
                </dataObjects>
            </PublicationDelivery>
            """.trimIndent()

        val inputDir = Files.createDirectories(tempDir.resolve("input-tie")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output-tie")).toFile()
        File(inputDir, "tie.xml").writeText(tieXml)

        NetexProcessor(filterConfig = filterConfig).run(inputDir, outputDir)

        val operator = plugin.getMostCommonOperator("TEST:Line:TIE")
        // Both operators have count=1, so alphabetically first should win
        assertThat(operator).isEqualTo("TEST:Operator:OPA")
    }
}

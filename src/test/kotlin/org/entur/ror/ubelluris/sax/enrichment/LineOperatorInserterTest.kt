package org.entur.ror.ubelluris.sax.enrichment

import org.assertj.core.api.Assertions.assertThat
import org.entur.netex.tools.lib.NetexProcessor
import org.entur.netex.tools.lib.config.FilterConfigBuilder
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.LineOperatorEnricher
import org.jdom2.Element
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class LineOperatorInserterTest {
    @TempDir
    lateinit var tempDir: Path

    private val saxBuilder = SAXBuilder()

    private fun processAndEnrich(vararg resourceFileNames: String): File {
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

        // Now enrich the output files
        val inserter = LineOperatorInserter(plugin)
        val outputFiles = outputDir.listFiles { file -> file.extension == "xml" }?.toList() ?: emptyList()

        outputFiles.forEach { file ->
            inserter.insert(file.toPath())
        }

        return outputFiles.first()
    }

    private fun findLineElement(
        xmlFile: File,
        lineId: String,
    ): Element? {
        val document = saxBuilder.build(xmlFile)
        val namespace = document.rootElement.namespace

        val linesIterator = document.rootElement.getDescendants(Filters.element(NetexTypes.LINE, namespace))
        while (linesIterator.hasNext()) {
            val lineElement = linesIterator.next()
            if (lineElement.getAttributeValue("id") == lineId) {
                return lineElement
            }
        }
        return null
    }

    private fun getOperatorRefForLine(
        xmlFile: File,
        lineId: String,
    ): String? {
        val lineElement = findLineElement(xmlFile, lineId) ?: return null
        val namespace = lineElement.namespace
        val operatorRef = lineElement.getChild(NetexTypes.OPERATOR_REF, namespace)
        return operatorRef?.getAttributeValue("ref")
    }

    private fun lineHasOperatorRef(
        xmlFile: File,
        lineId: String,
    ): Boolean {
        val lineElement = findLineElement(xmlFile, lineId) ?: return false
        val namespace = lineElement.namespace
        return lineElement.getChild(NetexTypes.OPERATOR_REF, namespace) != null
    }

    @Test
    fun shouldInsertOperatorRefForLineWithSingleOperator() {
        val outputFile = processAndEnrich("line-operator-single.xml")

        // Line 1 should now have OperatorRef
        assertThat(lineHasOperatorRef(outputFile, "TEST:Line:1")).isTrue()
        assertThat(getOperatorRefForLine(outputFile, "TEST:Line:1")).isEqualTo("TEST:Operator:OP1")
    }

    @Test
    fun shouldInsertMostCommonOperatorForLineWithMultipleOperators() {
        val outputFile = processAndEnrich("line-operator-multiple.xml")

        // Line 1 should have OP1 (5 occurrences vs 3 for OP2)
        assertThat(lineHasOperatorRef(outputFile, "TEST:Line:1")).isTrue()
        assertThat(getOperatorRefForLine(outputFile, "TEST:Line:1")).isEqualTo("TEST:Operator:OP1")
    }

    @Test
    fun shouldNotModifyLinesThatAlreadyHaveOperatorRef() {
        val outputFile = processAndEnrich("line-operator-multiple.xml")

        // Line 2 already has an operator, should remain unchanged
        assertThat(lineHasOperatorRef(outputFile, "TEST:Line:2")).isTrue()
        assertThat(getOperatorRefForLine(outputFile, "TEST:Line:2")).isEqualTo("TEST:Operator:EXISTING")
    }

    @Test
    fun shouldNotInsertOperatorRefForLineWithoutServiceJourneys() {
        val noSjXml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex" version="1.0">
                <PublicationTimestamp>2026-08-24T10:00:00</PublicationTimestamp>
                <dataObjects>
                    <CompositeFrame id="TEST:CompositeFrame:1" version="1">
                        <frames>
                            <ServiceFrame id="TEST:ServiceFrame:1" version="1">
                                <lines>
                                    <Line id="TEST:Line:ORPHAN" version="1">
                                        <Name>Orphan Line</Name>
                                        <TransportMode>bus</TransportMode>
                                    </Line>
                                </lines>
                            </ServiceFrame>
                        </frames>
                    </CompositeFrame>
                </dataObjects>
            </PublicationDelivery>
            """.trimIndent()

        val plugin = LineOperatorEnricher()
        val filterConfig =
            FilterConfigBuilder()
                .withPlugins(listOf(plugin))
                .build()

        val inputDir = Files.createDirectories(tempDir.resolve("input-orphan")).toFile()
        val outputDir = Files.createDirectories(tempDir.resolve("output-orphan")).toFile()
        File(inputDir, "orphan.xml").writeText(noSjXml)

        NetexProcessor(filterConfig = filterConfig).run(inputDir, outputDir)

        val inserter = LineOperatorInserter(plugin)
        val outputFile = outputDir.listFiles()?.first { it.extension == "xml" }!!
        inserter.insert(outputFile.toPath())

        // Orphan line should not have OperatorRef inserted
        assertThat(lineHasOperatorRef(outputFile, "TEST:Line:ORPHAN")).isFalse()
    }

    @Test
    fun shouldInsertOperatorRefAtCorrectPositionInLineElement() {
        val outputFile = processAndEnrich("line-operator-single.xml")

        println(outputFile.readText())

        val lineElement = findLineElement(outputFile, "TEST:Line:1")!!
        val entityNames = lineElement.children.map { it.name }
        val operatorRefIndex = entityNames.indexOf(NetexTypes.OPERATOR_REF)
        val transportModeIndex = entityNames.indexOf("TransportMode")

        assertThat(operatorRefIndex).isGreaterThan(transportModeIndex)
    }
}

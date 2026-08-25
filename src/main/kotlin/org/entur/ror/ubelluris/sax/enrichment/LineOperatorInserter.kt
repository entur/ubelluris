package org.entur.ror.ubelluris.sax.enrichment

import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.LineOperatorEnricher
import org.jdom2.Element
import org.jdom2.Namespace
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Inserts OperatorRef into Line elements based on data collected from ServiceJourneys.
 *
 * This works as a post-processing step after SAX filtering:
 * 1. Reads the filtered timetable file
 * 2. Finds Line elements without OperatorRef
 * 3. Looks up the most appropriate operator from LineOperatorEnricher
 * 4. Inserts the OperatorRef element
 * 5. Writes the modified file back
 */
class LineOperatorInserter(
    private val lineOperatorEnricher: LineOperatorEnricher,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val saxBuilder = SAXBuilder()

    fun insert(timetableXmlPath: Path): Path {
        log.info("Inserting operator references for Lines in {}", timetableXmlPath.fileName)

        val document = saxBuilder.build(timetableXmlPath.toFile())
        val root = document.rootElement
        val namespace = root.namespace

        // Find all Line elements
        val linesIterator = root.getDescendants(Filters.element(NetexTypes.LINE, namespace))
        val lines = mutableListOf<Element>()
        while (linesIterator.hasNext()) {
            lines.add(linesIterator.next())
        }

        var enrichedCount = 0
        var skippedAlreadyHasOperator = 0
        var skippedNoDataAvailable = 0

        lines.forEach { lineElement ->
            val lineId = lineElement.getAttributeValue("id") ?: return@forEach

            // Check if Line already has an OperatorRef
            val existingOperatorRef = lineElement.getChild(NetexTypes.OPERATOR_REF, namespace)
            if (existingOperatorRef != null) {
                skippedAlreadyHasOperator++
                return@forEach
            }

            // Get the most appropriate operator for this Line
            val operatorRef = lineOperatorEnricher.getMostCommonOperator(lineId)
            if (operatorRef == null) {
                skippedNoDataAvailable++
                return@forEach
            }

            // Insert the OperatorRef element
            insertOperatorRef(lineElement, namespace, operatorRef)
            enrichedCount++

            log.debug("Enriched Line {} with OperatorRef {}", lineId, operatorRef)
        }

        // Write the modified document back
        val outputter = XMLOutputter(Format.getPrettyFormat())
        Files.newBufferedWriter(timetableXmlPath).use { writer ->
            outputter.output(document, writer)
        }

        log.info(
            "Line operator enrichment complete: {} enriched, {} already had operator, {} no data available",
            enrichedCount,
            skippedAlreadyHasOperator,
            skippedNoDataAvailable,
        )

        return timetableXmlPath
    }

    /**
     * Inserts an OperatorRef element into a Line element.
     *
     * The OperatorRef is inserted right after the Name element if present,
     * or at the beginning of the Line element otherwise.
     */
    private fun insertOperatorRef(
        lineElement: Element,
        namespace: Namespace,
        operatorRef: String,
    ) {
        val operatorRefElement = Element(NetexTypes.OPERATOR_REF, namespace)
        operatorRefElement.setAttribute("ref", operatorRef)

        // Try to insert after Name element for better structure
        val nameElement = lineElement.getChild("Name", namespace)
        if (nameElement != null) {
            val index = lineElement.indexOf(nameElement)
            lineElement.addContent(index + 1, operatorRefElement)
        } else {
            // Otherwise insert at the beginning
            lineElement.addContent(0, operatorRefElement)
        }
    }
}

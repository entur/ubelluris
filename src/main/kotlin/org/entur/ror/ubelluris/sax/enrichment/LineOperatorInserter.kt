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

    private fun insertOperatorRef(
        lineElement: Element,
        namespace: Namespace,
        operatorRef: String,
    ) {
        // AuthorityRef and OperatorRef are mutually exclusive; skip if AuthorityRef exists
        if (lineElement.getChild("AuthorityRef", namespace) != null) {
            log.debug("Line {} already has AuthorityRef, skipping OperatorRef insertion", lineElement.getAttributeValue("id"))
            return
        }

        if (lineElement.getChild("OperatorRef", namespace) != null) {
            log.debug("Line {} already has OperatorRef, skipping OperatorRef insertion", lineElement.getAttributeValue("id"))
            return
        }

        val operatorRefElement = Element(NetexTypes.OPERATOR_REF, namespace)
        operatorRefElement.setAttribute("ref", operatorRef)

        val entityOrder =
            listOf(
                "Name",
                "ShortName",
                "Description",
                "TransportMode",
                "TransportSubmode",
                "Url",
                "PublicCode",
                "PrivateCode",
                "ExternalLineRef",
                "OperatorRef",
                "additionalOperators",
                "otherModes",
                "OperationalContextRef",
                "LineType",
                "TypeOfLineRef",
                "ExternalProductCategoryRef",
                "TypeOfProductCategoryRef",
                "TypeOfServiceRef",
                "Monitored",
                "routes",
                "RepresentedByGroupRef",
                "Presentation",
                "AlternativePresentation",
                "PrintedPresentation",
                "PaymentMethods",
                "typesOfPaymentMethod",
                "PurchaseMoment",
                "ContactDetails",
                "AccessibilityAssessment",
                "allowedDirections",
                "noticeAssignments",
                "documentLinks",
            )

        val operatorRefIndex = entityOrder.indexOf("OperatorRef")

        val referenceElement =
            lineElement.children.firstOrNull {
                entityOrder.indexOf(it.name) > operatorRefIndex
            }

        if (referenceElement != null) {
            val insertIndex = lineElement.indexOf(referenceElement)
            lineElement.addContent(insertIndex, operatorRefElement)
        } else {
            lineElement.addContent(operatorRefElement)
        }
    }
}

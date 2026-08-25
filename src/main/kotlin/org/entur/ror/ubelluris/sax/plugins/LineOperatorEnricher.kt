package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import java.io.File

/**
 * Collects operator references from ServiceJourneys and maps them to their Lines.
 * This data can be used to enrich Lines that are missing OperatorRef by copying
 * the most common operator from their ServiceJourneys.
 */
class LineOperatorEnricher : AbstractNetexPlugin() {
    private val log = LoggerFactory.getLogger(javaClass)

    // Tracks the current ServiceJourney's LineRef and OperatorRef as we parse
    private var currentServiceJourneyId: String? = null
    private var currentLineRef: String? = null
    private var currentOperatorRef: String? = null

    // Maps: Line ID -> Map<Operator ID, count>
    private val lineToOperatorCounts = mutableMapOf<String, MutableMap<String, Int>>()

    override fun getName(): String = javaClass.name

    override fun getDescription(): String = "Collects operator references from ServiceJourneys to enrich Lines missing OperatorRef"

    override fun getSupportedElementTypes() =
        setOf(
            NetexTypes.SERVICE_JOURNEY,
            "${NetexTypes.SERVICE_JOURNEY}/${NetexTypes.LINE_REF}",
            "${NetexTypes.SERVICE_JOURNEY}/${NetexTypes.OPERATOR_REF}",
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        when (elementName) {
            NetexTypes.SERVICE_JOURNEY -> {
                // Reset state for new ServiceJourney
                currentServiceJourneyId = currentEntity?.id
                currentLineRef = null
                currentOperatorRef = null
            }
            NetexTypes.LINE_REF -> {
                currentLineRef = attributes?.getValue("ref")
            }
            NetexTypes.OPERATOR_REF -> {
                currentOperatorRef = attributes?.getValue("ref")
            }
        }
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        if (elementName == NetexTypes.SERVICE_JOURNEY) {
            // When we finish parsing a ServiceJourney, record the mapping
            val lineRef = currentLineRef
            val operatorRef = currentOperatorRef

            if (lineRef != null && operatorRef != null) {
                val operatorCounts = lineToOperatorCounts.getOrPut(lineRef) { mutableMapOf() }
                operatorCounts[operatorRef] = operatorCounts.getOrDefault(operatorRef, 0) + 1
            }

            // Clear state
            currentServiceJourneyId = null
            currentLineRef = null
            currentOperatorRef = null
        }
    }

    override fun endDocument(file: File) {
        // Log statistics about what we found
        if (lineToOperatorCounts.isNotEmpty()) {
            log.info("Found ${lineToOperatorCounts.size} lines with operator references from ServiceJourneys in ${file.name}")

            // Log warnings for lines with multiple operators
            lineToOperatorCounts.forEach { (lineId, operatorCounts) ->
                if (operatorCounts.size > 1) {
                    val sortedOperators =
                        operatorCounts.entries
                            .sortedByDescending { it.value }
                            .joinToString(", ") { "${it.key} (${it.value} ServiceJourneys)" }
                    log.warn(
                        "Line $lineId has multiple operators: $sortedOperators. " +
                            "Will use most common operator if Line is missing OperatorRef.",
                    )
                }
            }
        }
    }

    /**
     * Returns the collected data: Map of Line ID -> Map of Operator ID to occurrence count.
     */
    override fun getCollectedData(): Map<String, Map<String, Int>> = lineToOperatorCounts

    /**
     * Determines the most appropriate operator for a given Line based on ServiceJourney data.
     * Returns null if no operator data is available for this line.
     *
     * Strategy:
     * - If all ServiceJourneys use the same operator, return that operator
     * - If multiple operators exist, return the one with the highest occurrence count
     * - In case of a tie, return the alphabetically first operator (for determinism)
     */
    fun getMostCommonOperator(lineId: String): String? {
        val operatorCounts = lineToOperatorCounts[lineId] ?: return null

        if (operatorCounts.isEmpty()) return null
        if (operatorCounts.size == 1) return operatorCounts.keys.first()

        // Find the max count
        val maxCount = operatorCounts.values.maxOrNull() ?: return null

        // Get all operators with the max count, sorted alphabetically for determinism
        val topOperators = operatorCounts.filter { it.value == maxCount }.keys.sorted()

        return topOperators.first()
    }
}

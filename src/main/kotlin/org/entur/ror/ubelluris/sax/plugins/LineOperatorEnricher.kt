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
 *
 * Handles the standard NeTEx pattern:
 * ServiceJourney → JourneyPatternRef → JourneyPattern → RouteRef → Route → LineRef
 */
class LineOperatorEnricher : AbstractNetexPlugin() {
    private val log = LoggerFactory.getLogger(javaClass)

    // Tracks the current ServiceJourney's JourneyPatternRef and OperatorRef as we parse
    private var currentJourneyPatternRef: String? = null
    private var currentOperatorRef: String? = null

    // Mappings to resolve the reference chain
    private val journeyPatternToRoute = mutableMapOf<String, String>()
    private val routeToLine = mutableMapOf<String, String>()

    // Maps: Line ID -> Map<Operator ID, count>
    private val lineToOperatorCounts = mutableMapOf<String, MutableMap<String, Int>>()

    override fun getName(): String = javaClass.name

    override fun getDescription(): String = "Collects operator references from ServiceJourneys to enrich Lines missing OperatorRef"

    override fun getSupportedElementTypes() =
        setOf(
            // ServiceJourney and its children
            NetexTypes.SERVICE_JOURNEY,
            "${NetexTypes.SERVICE_JOURNEY}/${NetexTypes.JOURNEY_PATTERN_REF}",
            "${NetexTypes.SERVICE_JOURNEY}/${NetexTypes.OPERATOR_REF}",
            // JourneyPattern → Route mapping
            "${NetexTypes.JOURNEY_PATTERN}/${NetexTypes.ROUTE_REF}",
            // Route → Line mapping
            "${NetexTypes.ROUTE}/${NetexTypes.LINE_REF}",
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        when (elementName) {
            NetexTypes.SERVICE_JOURNEY -> {
                // Reset state for new ServiceJourney
                currentJourneyPatternRef = null
                currentOperatorRef = null
            }
            NetexTypes.LINE_REF -> {
                // Route → Line mapping
                if (currentEntity?.type == NetexTypes.ROUTE) {
                    val ref = attributes?.getValue("ref") ?: return
                    val routeId = currentEntity.id
                    routeToLine[routeId] = ref
                }
            }
            NetexTypes.JOURNEY_PATTERN_REF -> {
                // ServiceJourney → JourneyPatternRef
                currentJourneyPatternRef = attributes?.getValue("ref")
            }
            NetexTypes.ROUTE_REF -> {
                // JourneyPattern → RouteRef
                val ref = attributes?.getValue("ref") ?: return
                val journeyPatternId = currentEntity?.id ?: return
                journeyPatternToRoute[journeyPatternId] = ref
            }
            NetexTypes.OPERATOR_REF -> {
                // ServiceJourney → OperatorRef
                if (currentEntity?.type == NetexTypes.SERVICE_JOURNEY) {
                    currentOperatorRef = attributes?.getValue("ref")
                }
            }
        }
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        if (elementName == NetexTypes.SERVICE_JOURNEY) {
            // Resolve ServiceJourney → JourneyPattern → Route → Line
            val operatorRef = currentOperatorRef
            val journeyPatternRef = currentJourneyPatternRef

            if (operatorRef != null && journeyPatternRef != null) {
                val routeRef = journeyPatternToRoute[journeyPatternRef]
                if (routeRef != null) {
                    val lineRef = routeToLine[routeRef]
                    if (lineRef != null) {
                        val operatorCounts = lineToOperatorCounts.getOrPut(lineRef) { mutableMapOf() }
                        operatorCounts[operatorRef] = operatorCounts.getOrDefault(operatorRef, 0) + 1
                    }
                }
            }

            // Clear state
            currentJourneyPatternRef = null
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

package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.data.StopPointInJourneyPatternData
import org.entur.ror.ubelluris.sax.plugins.data.StopPointInJourneyPatternKey
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import java.io.File

/**
 * Collects all StopPointInJourneyPattern elements per JourneyPattern, so that the first and last
 * stop point of each journey pattern can be identified (e.g. to correct ForAlighting/ForBoarding).
 *
 * Stop points whose key occurs in more than one document are treated as collisions and excluded
 * from the first/last results, since the output handler cannot tell which document it is writing.
 */
open class JourneyPatternStopPointCollectorPlugin : AbstractNetexPlugin() {
    private val logger = LoggerFactory.getLogger(javaClass)

    // JourneyPattern ID -> stop points in document order, for the document currently being parsed
    private val stopPointsByJourneyPattern = mutableMapOf<String, MutableList<StopPointInJourneyPatternData>>()

    // JourneyPatterns in the current document with a StopPointInJourneyPattern lacking a valid order
    private val invalidJourneyPatternIds = mutableSetOf<String>()

    private val documentsByKey = mutableMapOf<StopPointInJourneyPatternKey, MutableSet<String>>()
    private val firstStopPoints = mutableSetOf<StopPointInJourneyPatternKey>()
    private val lastStopPoints = mutableSetOf<StopPointInJourneyPatternKey>()

    private var currentJourneyPatternId: String? = null

    override fun getName(): String = javaClass.name

    override fun getDescription(): String = "Collects StopPointInJourneyPattern elements per JourneyPattern"

    override fun getSupportedElementTypes() =
        setOf(
            NetexTypes.JOURNEY_PATTERN,
            NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN,
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        when (elementName) {
            NetexTypes.JOURNEY_PATTERN -> {
                currentJourneyPatternId = attributes?.getValue("id")
            }
            NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN -> {
                val jpId = currentJourneyPatternId ?: return
                val id = attributes?.getValue("id") ?: return
                val order = attributes.getValue("order")
                val sortOrder = order?.toIntOrNull()
                if (sortOrder == null) {
                    logger.warn(
                        "StopPointInJourneyPattern {} in JourneyPattern {} has missing or invalid order '{}'; " +
                            "ForAlighting/ForBoarding will not be corrected for this JourneyPattern",
                        id,
                        jpId,
                        order,
                    )
                    invalidJourneyPatternIds.add(jpId)
                    return
                }
                stopPointsByJourneyPattern.getOrPut(jpId) { mutableListOf() }.add(
                    StopPointInJourneyPatternData(
                        key = StopPointInJourneyPatternKey(jpId, id, attributes.getValue("version"), order),
                        sortOrder = sortOrder,
                    ),
                )
            }
        }
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        if (elementName == NetexTypes.JOURNEY_PATTERN) {
            currentJourneyPatternId = null
        }
    }

    override fun endDocument(file: File) = finishDocument(file.name)

    override fun endDocument(documentName: String) = finishDocument(documentName)

    private fun finishDocument(documentName: String) {
        stopPointsByJourneyPattern.forEach { (jpId, stopPoints) ->
            if (jpId !in invalidJourneyPatternIds) {
                stopPoints.minByOrNull { it.sortOrder }?.let { firstStopPoints.add(it.key) }
                stopPoints.maxByOrNull { it.sortOrder }?.let { lastStopPoints.add(it.key) }
            }
            stopPoints.forEach { stopPoint ->
                val documents = documentsByKey.getOrPut(stopPoint.key) { mutableSetOf() }
                if (documents.isNotEmpty() && documentName !in documents) {
                    logger.warn(
                        "StopPointInJourneyPattern {} occurs in multiple documents ({}, {}); ForAlighting/ForBoarding will not be corrected",
                        stopPoint.key,
                        documents.joinToString(),
                        documentName,
                    )
                }
                documents.add(documentName)
            }
        }
        stopPointsByJourneyPattern.clear()
        invalidJourneyPatternIds.clear()
        currentJourneyPatternId = null
    }

    override fun getCollectedData(): Map<StopPointInJourneyPatternKey, Set<String>> = documentsByKey

    /** The first StopPointInJourneyPattern (lowest order) of each JourneyPattern, excluding collisions. */
    fun getFirstStopPoints(): Set<StopPointInJourneyPatternKey> = firstStopPoints.filterTo(mutableSetOf()) { !isCollision(it) }

    /** The last StopPointInJourneyPattern (highest order) of each JourneyPattern, excluding collisions. */
    fun getLastStopPoints(): Set<StopPointInJourneyPatternKey> = lastStopPoints.filterTo(mutableSetOf()) { !isCollision(it) }

    private fun isCollision(key: StopPointInJourneyPatternKey) = (documentsByKey[key]?.size ?: 0) > 1
}

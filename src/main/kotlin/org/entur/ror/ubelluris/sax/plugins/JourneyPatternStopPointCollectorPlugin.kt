package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.data.StopPointInJourneyPatternData
import org.xml.sax.Attributes

/**
 * Collects all StopPointInJourneyPattern elements per JourneyPattern, so that the first and last
 * stop point of each journey pattern can be identified (e.g. to correct ForAlighting/ForBoarding).
 */
open class JourneyPatternStopPointCollectorPlugin : AbstractNetexPlugin() {
    // JourneyPattern ID -> stop points in document order
    private val stopPointsByJourneyPattern = mutableMapOf<String, MutableList<StopPointInJourneyPatternData>>()

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
                val stopPoints = stopPointsByJourneyPattern.getOrPut(jpId) { mutableListOf() }
                stopPoints.add(
                    StopPointInJourneyPatternData(
                        id = id,
                        journeyPatternId = jpId,
                        order = attributes.getValue("order")?.toIntOrNull() ?: stopPoints.size,
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

    override fun getCollectedData(): Map<String, List<StopPointInJourneyPatternData>> = stopPointsByJourneyPattern

    /** IDs of the first StopPointInJourneyPattern (lowest order) in each JourneyPattern. */
    fun getFirstStopPointIds(): Set<String> =
        stopPointsByJourneyPattern.values.mapNotNullTo(mutableSetOf()) { stopPoints ->
            stopPoints.minByOrNull { it.order }?.id
        }

    /** IDs of the last StopPointInJourneyPattern (highest order) in each JourneyPattern. */
    fun getLastStopPointIds(): Set<String> =
        stopPointsByJourneyPattern.values.mapNotNullTo(mutableSetOf()) { stopPoints ->
            stopPoints.maxByOrNull { it.order }?.id
        }
}

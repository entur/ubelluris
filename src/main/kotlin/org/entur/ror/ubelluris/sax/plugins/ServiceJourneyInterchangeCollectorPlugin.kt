package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.data.ServiceJourneyInterchangeData
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes

/**
 * Plugin that collects ServiceJourneyInterchange content for deduplication comparison.
 */
open class ServiceJourneyInterchangeCollectorPlugin : AbstractNetexPlugin() {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Map of interchange ID -> list of all occurrences with their full content
    private val interchangeData = mutableMapOf<String, MutableList<ServiceJourneyInterchangeData>>()

    // Current interchange being built
    private var currentInterchangeId: String? = null
    private var currentInterchangeVersion: String? = null
    private val currentTextBuffer = StringBuilder()
    private var currentFieldName: String? = null

    // Fields we're capturing from the current interchange
    private var priority: String? = null
    private var guaranteed: String? = null
    private var advertised: String? = null
    private var fromPointRef: String? = null
    private var toPointRef: String? = null
    private var fromJourneyRef: String? = null
    private var toJourneyRef: String? = null

    override fun getName(): String = javaClass.name

    override fun getDescription(): String = "Collects ServiceJourneyInterchange content for deduplication comparison"

    override fun getSupportedElementTypes() =
        setOf(
            NetexTypes.SERVICE_JOURNEY_INTERCHANGE,
            "${NetexTypes.SERVICE_JOURNEY_INTERCHANGE}/Priority",
            "${NetexTypes.SERVICE_JOURNEY_INTERCHANGE}/Guaranteed",
            "${NetexTypes.SERVICE_JOURNEY_INTERCHANGE}/Advertised",
            "${NetexTypes.SERVICE_JOURNEY_INTERCHANGE}/FromPointRef",
            "${NetexTypes.SERVICE_JOURNEY_INTERCHANGE}/ToPointRef",
            "${NetexTypes.SERVICE_JOURNEY_INTERCHANGE}/FromJourneyRef",
            "${NetexTypes.SERVICE_JOURNEY_INTERCHANGE}/ToJourneyRef",
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        when (elementName) {
            NetexTypes.SERVICE_JOURNEY_INTERCHANGE -> {
                currentInterchangeId = attributes?.getValue("id")
                currentInterchangeVersion = attributes?.getValue("version")
                priority = null
                guaranteed = null
                advertised = null
                fromPointRef = null
                toPointRef = null
                fromJourneyRef = null
                toJourneyRef = null
            }
            "Priority" -> {
                currentTextBuffer.clear()
                currentFieldName = "Priority"
            }
            "Guaranteed" -> {
                currentTextBuffer.clear()
                currentFieldName = "Guaranteed"
            }
            "Advertised" -> {
                currentTextBuffer.clear()
                currentFieldName = "Advertised"
            }
            "FromPointRef" -> {
                fromPointRef = attributes?.getValue("ref")
            }
            "ToPointRef" -> {
                toPointRef = attributes?.getValue("ref")
            }
            "FromJourneyRef" -> {
                fromJourneyRef = attributes?.getValue("ref")
            }
            "ToJourneyRef" -> {
                toJourneyRef = attributes?.getValue("ref")
            }
        }
    }

    override fun characters(
        elementName: String,
        ch: CharArray?,
        start: Int,
        length: Int,
    ) {
        if (ch != null && currentFieldName != null) {
            currentTextBuffer.appendRange(ch, start, start + length)
        }
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        when (elementName) {
            "Priority" -> {
                priority = currentTextBuffer.toString().trim()
                currentFieldName = null
            }
            "Guaranteed" -> {
                guaranteed = currentTextBuffer.toString().trim()
                currentFieldName = null
            }
            "Advertised" -> {
                advertised = currentTextBuffer.toString().trim()
                currentFieldName = null
            }
            NetexTypes.SERVICE_JOURNEY_INTERCHANGE -> {
                val id = currentInterchangeId
                if (id != null) {
                    val data =
                        ServiceJourneyInterchangeData(
                            id = id,
                            version = currentInterchangeVersion,
                            priority = priority,
                            guaranteed = guaranteed,
                            advertised = advertised,
                            fromPointRef = fromPointRef,
                            toPointRef = toPointRef,
                            fromJourneyRef = fromJourneyRef,
                            toJourneyRef = toJourneyRef,
                        )
                    interchangeData.getOrPut(id) { mutableListOf() }.add(data)
                }
                currentInterchangeId = null
                currentInterchangeVersion = null
            }
        }
    }

    override fun getCollectedData(): Map<String, List<ServiceJourneyInterchangeData>> = interchangeData

    /**
     * Returns interchange IDs that have duplicates with identical content.
     * These should keep one instance.
     */
    fun getIdenticalDuplicates(): Set<String> {
        val identical = mutableSetOf<String>()
        interchangeData.forEach { (id, occurrences) ->
            if (occurrences.size > 1) {
                // Check if all occurrences have identical content
                val firstContent = occurrences.first().copy(id = "", version = "")
                val allIdentical = occurrences.all { it.copy(id = "", version = "") == firstContent }
                if (allIdentical) {
                    identical.add(id)
                }
            }
        }
        return identical
    }

    /**
     * Returns interchange IDs that have duplicates with different content.
     * These should be removed entirely.
     */
    fun getConflictingDuplicates(): Set<String> {
        val conflicting = mutableSetOf<String>()
        interchangeData.forEach { (id, occurrences) ->
            if (occurrences.size > 1) {
                // Check if any occurrences have different content
                val firstContent = occurrences.first().copy(id = "", version = "")
                val anyDifferent = occurrences.any { it.copy(id = "", version = "") != firstContent }
                if (anyDifferent) {
                    conflicting.add(id)
                }
            }
        }
        return conflicting
    }
}

package org.entur.ror.ubelluris.sax.plugins

import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.TransportMode
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes

/**
 * Plugin that filters out Line entities based on their TransportMode.
 * When a Line's TransportMode is one of the provided modes, the Line is marked
 * for removal and stored in the LineFilteringRepository.
 *
 * @property transportModesToRemove TransportModes (e.g. TAXI) that cause a Line to be removed
 * @property repository Repository to add Line IDs to, may be shared with other line filter plugins
 */
class LineTransportModeFilterPlugin(
    private val transportModesToRemove: Set<TransportMode>,
    val repository: LineFilteringRepository,
) : AbstractNetexPlugin() {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val stringBuilder = StringBuilder()
    private var currentLineId: String? = null
    private var isProcessingTransportMode = false

    override fun getName(): String = "LineTransportModeFilterPlugin"

    override fun getDescription(): String = "Filters out Lines based on their TransportMode"

    override fun getSupportedElementTypes(): Set<String> =
        setOf(
            NetexTypes.LINE,
            NetexTypes.TRANSPORT_MODE,
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        val entity = currentEntity ?: return
        when (elementName) {
            NetexTypes.LINE -> {
                currentLineId = entity.id
                isProcessingTransportMode = false
            }
            NetexTypes.TRANSPORT_MODE -> {
                if (entity.type == NetexTypes.LINE) {
                    isProcessingTransportMode = true
                    stringBuilder.clear()
                }
            }
        }
    }

    override fun characters(
        elementName: String,
        ch: CharArray?,
        start: Int,
        length: Int,
    ) {
        if (elementName == NetexTypes.TRANSPORT_MODE && isProcessingTransportMode && ch != null) {
            stringBuilder.appendRange(ch, start, start + length)
        }
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        if (currentEntity == null) return
        when (elementName) {
            NetexTypes.TRANSPORT_MODE -> {
                val lineId = currentLineId
                if (isProcessingTransportMode && lineId != null) {
                    val transportMode = TransportMode.fromNetexValue(stringBuilder.toString().trim())
                    if (transportMode != null && transportMode in transportModesToRemove) {
                        repository.addLineId(lineId)
                        logger.info(
                            "Filtering Line with TransportMode",
                            kv("lineId", lineId),
                            kv("transportMode", transportMode.netexValue),
                        )
                    }
                    stringBuilder.clear()
                    isProcessingTransportMode = false
                }
            }
            NetexTypes.LINE -> {
                currentLineId = null
                isProcessingTransportMode = false
            }
        }
    }

    override fun getCollectedData(): LineFilteringRepository = repository
}

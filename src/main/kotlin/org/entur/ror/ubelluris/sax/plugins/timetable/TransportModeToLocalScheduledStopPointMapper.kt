package org.entur.ror.ubelluris.sax.plugins.timetable

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.TransportMode
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import java.io.File

class TransportModeToLocalScheduledStopPointMapper(private val transportModes: List<TransportMode>) : AbstractNetexPlugin() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val modeTextBuffer = StringBuilder()
    private val providerIdToModes = mutableMapOf<String, MutableSet<TransportMode>>()
    private val lineToTransportMode = mutableMapOf<String, TransportMode>()
    private val routeToLine = mutableMapOf<String, String>()
    private val journyPatternToRoute = mutableMapOf<String, String>()
    private val journyPatternToScheduledStopPoint = mutableMapOf<String, MutableSet<String>>()

    override fun getName(): String = javaClass.name

    override fun getDescription(): String =
        "Extracts provider stop point IDs and their transport modes from timetable files"

    override fun getSupportedElementTypes() = setOf(
        "${NetexTypes.ROUTE}/${NetexTypes.LINE_REF}",
        "${NetexTypes.LINE}/${NetexTypes.TRANSPORT_MODE}",
        "${NetexTypes.JOURNEY_PATTERN}/${NetexTypes.ROUTE_REF}",
        "${NetexTypes.STOP_POINT_IN_JOURNEY_PATTERN}/${NetexTypes.SCHEDULED_STOP_POINT_REF}",
    )

    override fun startElement(elementName: String, attributes: Attributes?, currentEntity: Entity?) {
        when (elementName) {
            NetexTypes.LINE_REF -> {
                val ref = attributes?.getValue("ref") ?: return
                val routeId = currentEntity?.id ?: return
                routeToLine[routeId] = ref
            }
            NetexTypes.TRANSPORT_MODE -> modeTextBuffer.clear()
            NetexTypes.ROUTE_REF -> {
                val ref = attributes?.getValue("ref") ?: return
                val jpId = currentEntity?.id ?: return
                journyPatternToRoute[jpId] = ref
            }
            NetexTypes.SCHEDULED_STOP_POINT_REF -> {
                val ref = attributes?.getValue("ref") ?: return
                val localId = toLocalId(ref) ?: return
                val jpId = currentEntity?.parent?.id ?: return
                journyPatternToScheduledStopPoint.getOrPut(jpId) { mutableSetOf() }.add(localId)
            }
        }
    }

    override fun characters(elementName: String, ch: CharArray?, start: Int, length: Int) {
        if (elementName == NetexTypes.TRANSPORT_MODE && ch != null) {
            modeTextBuffer.appendRange(ch, start, start + length)
        }
    }

    override fun endElement(elementName: String, currentEntity: Entity?) {
        if (elementName == NetexTypes.TRANSPORT_MODE) {
            val mode = TransportMode.fromNetexValue(modeTextBuffer.toString())
            val transportMode = mode?.takeIf { it in transportModes }
            val lineId = currentEntity?.id
            if (transportMode != null && lineId != null) {
                lineToTransportMode[lineId] = transportMode
            }
        }
    }

    override fun endDocument(file: File) {
        for ((jpId, localIds) in journyPatternToScheduledStopPoint) {
            val routeId = journyPatternToRoute[jpId] ?: continue
            val lineId = routeToLine[routeId] ?: continue
            val mode = lineToTransportMode[lineId] ?: continue
            for (localId in localIds) {
                providerIdToModes.getOrPut(localId) { mutableSetOf() }.add(mode)
            }
        }
        lineToTransportMode.clear()
        routeToLine.clear()
        journyPatternToRoute.clear()
        journyPatternToScheduledStopPoint.clear()
    }

    override fun getCollectedData(): Map<String, Set<TransportMode>> = providerIdToModes

    /**
     * Converts a ScheduledStopPointRef value (e.g. "SE:001:ScheduledStopPoint:001")
     * to the codespace:id format used as GIDs in stop place keyList (e.g. "1:001").
     */
    private fun toLocalId(originalRef: String): String? {
        val parts = originalRef.split(":")
        if (parts.size < 4) return null
        val codespace = parts[1].trimStart('0').ifEmpty { "0" }
        val id = parts[3]
        return "$codespace:$id"
    }
}

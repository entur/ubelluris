package org.entur.ror.ubelluris.sax.plugins.handlers

import org.entur.netex.tools.lib.model.Entity
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingDataCollector
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.xml.sax.Attributes

class ParentSiteRefHandler(val stopPlacePurgingRepository: StopPlacePurgingRepository) : StopPlacePurgingDataCollector() {
    private val stringBuilder = StringBuilder()

    override fun startElement(context: StopPlacePurgingParsingContext, attributes: Attributes?, currentEntity: Entity) {
        stringBuilder.clear()
        val refValue = attributes?.getValue("ref")
        if (refValue != null) {
            stringBuilder.append(refValue)
        }
    }

    override fun characters(context: StopPlacePurgingParsingContext, ch: CharArray?, start: Int, length: Int) {
        stringBuilder.append(ch, start, length)
    }

    override fun endElement(context: StopPlacePurgingParsingContext, currentEntity: Entity) {
        if (stringBuilder.isNotEmpty()) {
            stopPlacePurgingRepository.addChildStopToParent(stringBuilder.toString(), currentEntity.id)
        }
        stringBuilder.clear()
    }
}
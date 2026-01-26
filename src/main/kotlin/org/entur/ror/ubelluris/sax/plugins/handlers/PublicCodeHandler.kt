package org.entur.ror.ubelluris.sax.plugins.handlers

import org.entur.netex.tools.lib.model.Entity
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingDataCollector
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.entur.ror.ubelluris.sax.plugins.data.QuayData
import org.xml.sax.Attributes

class PublicCodeHandler(val stopPlacePurgingRepository: StopPlacePurgingRepository) : StopPlacePurgingDataCollector() {
    private val stringBuilder = StringBuilder()

    override fun startElement(context: StopPlacePurgingParsingContext, attributes: Attributes?, currentEntity: Entity) {
        stringBuilder.clear()
    }

    override fun characters(context: StopPlacePurgingParsingContext, ch: CharArray?, start: Int, length: Int) {
        stringBuilder.append(ch, start, length)
    }

    override fun endElement(context: StopPlacePurgingParsingContext, currentEntity: Entity) {
        val publicCode = stringBuilder.toString()

        if (publicCode in listOf("81", "82", "83")) {
            stopPlacePurgingRepository.addEntityId(currentEntity.id)
        }

        val parentEntityId = currentEntity.parent?.id
        if (parentEntityId != null) {
            stopPlacePurgingRepository.addQuayToStopPlace(
                parentEntityId,
                QuayData(currentEntity.id, publicCode)
            )
            context.quayHasPublicCode = true
        }

        stringBuilder.clear()
    }
}
package org.entur.ror.ubelluris.sax.plugins.handlers

import org.entur.netex.tools.lib.model.Entity
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingDataCollector
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.entur.ror.ubelluris.sax.plugins.data.QuayData
import org.xml.sax.Attributes

class QuayTrackingHandler(
    val stopPlacePurgingRepository: StopPlacePurgingRepository
) : StopPlacePurgingDataCollector() {

    override fun startElement(
        context: StopPlacePurgingParsingContext,
        attributes: Attributes?,
        currentEntity: Entity
    ) {
        context.currentQuayId = currentEntity.id
        context.quayHasPublicCode = false
    }

    override fun endElement(
        context: StopPlacePurgingParsingContext,
        currentEntity: Entity
    ) {
        val quayId = context.currentQuayId
        val hasPublicCode = context.quayHasPublicCode

        if (!hasPublicCode && quayId != null) {
            val parentEntityId = currentEntity.parent?.id
            if (parentEntityId != null) {
                stopPlacePurgingRepository.addQuayToStopPlace(
                    parentEntityId,
                    QuayData(currentEntity.id, "")
                )
            }
        }

        context.currentQuayId = null
        context.quayHasPublicCode = false
    }
}

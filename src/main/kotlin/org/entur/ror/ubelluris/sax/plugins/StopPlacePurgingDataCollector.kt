package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.xml.sax.Attributes

abstract class StopPlacePurgingDataCollector {
    open fun characters(context: StopPlacePurgingParsingContext, ch: CharArray?, start: Int, length: Int) {}

    open fun endElement(context: StopPlacePurgingParsingContext, currentEntity: Entity) {}

    open fun startElement(context: StopPlacePurgingParsingContext, attributes: Attributes?, currentEntity: Entity) {}
}
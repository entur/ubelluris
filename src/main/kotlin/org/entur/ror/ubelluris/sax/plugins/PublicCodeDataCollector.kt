package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.xml.sax.Attributes

abstract class PublicCodeDataCollector {
    open fun characters(context: PublicCodeParsingContext, ch: CharArray?, start: Int, length: Int) {}

    open fun endElement(context: PublicCodeParsingContext, currentEntity: Entity) {}

    open fun startElement(context: PublicCodeParsingContext, attributes: Attributes?, currentEntity: Entity) {}
}
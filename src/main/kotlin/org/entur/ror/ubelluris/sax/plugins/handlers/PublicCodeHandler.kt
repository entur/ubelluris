package org.entur.ror.ubelluris.sax.plugins.handlers

import org.entur.netex.tools.lib.model.Entity
import org.entur.ror.ubelluris.sax.plugins.PublicCodeDataCollector
import org.entur.ror.ubelluris.sax.plugins.PublicCodeParsingContext
import org.entur.ror.ubelluris.sax.plugins.PublicCodeRepository
import org.xml.sax.Attributes

class PublicCodeHandler(val publicCodeRepository: PublicCodeRepository) : PublicCodeDataCollector() {
    private val stringBuilder = StringBuilder()

    override fun startElement(context: PublicCodeParsingContext, attributes: Attributes?, currentEntity: Entity) {
        stringBuilder.clear()
    }

    override fun characters(context: PublicCodeParsingContext, ch: CharArray?, start: Int, length: Int) {
        stringBuilder.append(ch, start, length)
    }

    override fun endElement(context: PublicCodeParsingContext, currentEntity: Entity) {
        publicCodeRepository.getType(currentEntity.id, stringBuilder.toString())
    }
}
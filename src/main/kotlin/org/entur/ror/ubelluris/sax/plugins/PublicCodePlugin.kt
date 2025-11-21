package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.sax.plugins.handlers.PublicCodeHandler
import org.xml.sax.Attributes

class PublicCodePlugin(
    val publicCodeRepository: PublicCodeRepository
) : AbstractNetexPlugin() {

    private val parsingContext: PublicCodeParsingContext = PublicCodeParsingContext()

    // Map of element handlers - delegating to existing collector implementations
    private val elementHandlers: Map<String, PublicCodeDataCollector> by lazy {
        mapOf(
            "PublicCode" to PublicCodeHandler(publicCodeRepository)
        )
    }

    override fun getName(): String = "PublicCodePlugin"

    override fun getDescription(): String =
        "Collects date-related data from NeTEx elements to enable date-based filtering of service journeys and related entities"

    override fun getSupportedElementTypes(): Set<String> = elementHandlers.keys.toSet()

    override fun startElement(elementName: String, attributes: Attributes?, currentEntity: Entity?) {
        currentEntity?.let { entity ->
            elementHandlers[elementName]?.startElement(parsingContext, attributes, entity)
        }
    }

    override fun characters(elementName: String, ch: CharArray?, start: Int, length: Int) {
        elementHandlers[elementName]?.characters(parsingContext, ch, start, length)
    }

    override fun endElement(elementName: String, currentEntity: Entity?) {
        currentEntity?.let { entity ->
            elementHandlers[elementName]?.endElement(parsingContext, entity)
        }
    }

    override fun getCollectedData(): PublicCodeRepository {
        return publicCodeRepository
    }
}
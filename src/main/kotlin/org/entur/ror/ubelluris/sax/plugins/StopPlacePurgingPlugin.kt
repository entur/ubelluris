package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.handlers.BlacklistQuayHandler
import org.entur.ror.ubelluris.sax.plugins.handlers.ParentSiteRefHandler
import org.entur.ror.ubelluris.sax.plugins.handlers.PublicCodeHandler
import org.xml.sax.Attributes

class StopPlacePurgingPlugin(
    val stopPlacePurgingRepository: StopPlacePurgingRepository,
    private val blacklistFilePath: String
) : AbstractNetexPlugin() {

    private val parsingContext: StopPlacePurgingParsingContext = StopPlacePurgingParsingContext()

    private val elementHandlers: Map<String, StopPlacePurgingDataCollector> by lazy {
        mapOf(
            NetexTypes.PUBLIC_CODE to PublicCodeHandler(stopPlacePurgingRepository),
            NetexTypes.QUAY to BlacklistQuayHandler(stopPlacePurgingRepository, java.io.File(blacklistFilePath)),
            NetexTypes.PARENT_SITE_REF to ParentSiteRefHandler(stopPlacePurgingRepository)
        )
    }

    override fun getName(): String = TODO("Not yet implemented")

    override fun getDescription(): String = TODO("Not yet implemented")

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

    override fun getCollectedData(): StopPlacePurgingRepository {
        return stopPlacePurgingRepository
    }
}
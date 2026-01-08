package org.entur.ror.ubelluris.sax.plugins.handlers

import org.entur.netex.tools.lib.model.Entity
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingDataCollector
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.xml.sax.Attributes
import java.io.File

class BlacklistQuayHandler(
    val stopPlacePurgingRepository: StopPlacePurgingRepository,
    private val blacklistFile: File
) : StopPlacePurgingDataCollector() {

    private val blacklistedQuayIds: Set<String> by lazy {
        loadBlacklistFile()
    }

    private fun loadBlacklistFile(): Set<String> {
        return if (blacklistFile.exists()) {
            blacklistFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        } else {
            emptySet()
        }
    }

    override fun startElement(context: StopPlacePurgingParsingContext, attributes: Attributes?, currentEntity: Entity) {
        val idValue = attributes?.getValue("id")
        if (idValue != null && idValue in blacklistedQuayIds) {
            stopPlacePurgingRepository.addEntityId(currentEntity.id)
        }
    }
}
package org.entur.ror.ubelluris.sax.plugins.handlers

import org.entur.netex.tools.lib.model.Entity
import org.entur.ror.ubelluris.sax.plugins.PublicCodeDataCollector
import org.entur.ror.ubelluris.sax.plugins.PublicCodeParsingContext
import org.entur.ror.ubelluris.sax.plugins.PublicCodeRepository
import org.xml.sax.Attributes
import java.io.File

class BlacklistQuayHandler(
    val publicCodeRepository: PublicCodeRepository,
    private val blacklistFile: File = File("processing/blacklist-quays.txt")
) : PublicCodeDataCollector() {

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

    override fun startElement(context: PublicCodeParsingContext, attributes: Attributes?, currentEntity: Entity) {
        val idValue = attributes?.getValue("id")
        if (idValue != null && idValue in blacklistedQuayIds) {
            publicCodeRepository.addEntityId(currentEntity.id)
        }
    }
}
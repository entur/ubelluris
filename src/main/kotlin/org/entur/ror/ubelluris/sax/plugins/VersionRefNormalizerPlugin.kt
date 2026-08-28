package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.sax.plugins.data.LocalEntityRegistry
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes

/**
 * Plugin that collects locally defined entity IDs and identifies references that incorrectly use
 * versionRef instead of version.
 *
 * According to NeTEx specification:
 * - `version` should be used for entities defined in the same document (local references)
 * - `versionRef` should only be used for external entities (defined in other documents)
 *
 * This plugin builds a registry of all locally defined entity IDs (entities with an 'id' attribute)
 * and makes it available to handlers that can fix incorrect versionRef usage.
 */
class VersionRefNormalizerPlugin : AbstractNetexPlugin() {
    private val logger = LoggerFactory.getLogger(javaClass)
    val registry = LocalEntityRegistry()

    // Track elements that have versionRef pointing to local entities (for logging/debugging)
    private val incorrectVersionRefs = mutableMapOf<String, MutableSet<String>>()

    override fun getName(): String = javaClass.name

    override fun getDescription(): String = "Collects local entity IDs and identifies incorrect versionRef usage for local entities"

    override fun getSupportedElementTypes() =
        setOf(
            "TrainNumber",
            "VehicleType"
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        if (attributes == null) return

        // Register any element with an 'id' attribute as a local entity
        val id = attributes.getValue("id")
        if (id != null) {
            registry.registerLocalEntity(id)
            logger.debug("Registered local entity: {} (type: {})", id, elementName)
        }

        // Check if this is a reference element with versionRef
        val ref = attributes.getValue("ref")
        val versionRef = attributes.getValue("versionRef")
        if (ref != null && versionRef != null) {
            // We'll check if this ref points to a local entity in endDocument
            // For now, just track that we found a versionRef
            incorrectVersionRefs.getOrPut(elementName) { mutableSetOf() }.add(ref)
        }
    }

    override fun endDocument(file: java.io.File) {
        // After parsing the entire document, check which versionRefs are incorrect
        var incorrectCount = 0
        incorrectVersionRefs.forEach { (elementType, refs) ->
            refs.forEach { ref ->
                if (registry.isLocalEntity(ref)) {
                    incorrectCount++
                    logger.debug(
                        "Found incorrect versionRef usage: {} element references local entity {}",
                        elementType,
                        ref,
                    )
                }
            }
        }

        if (incorrectCount > 0) {
            logger.info(
                "Found {} reference(s) using versionRef for local entities (should use 'version' instead) in file {}",
                incorrectCount,
                file.name,
            )
        }

        logger.info("Registered {} local entities in file {}", registry.getLocalEntityIds().size, file.name)
    }

    override fun getCollectedData(): LocalEntityRegistry = registry
}

package org.entur.ror.ubelluris.sax.plugins

import org.entur.netex.tools.lib.model.Entity
import org.entur.netex.tools.lib.plugin.AbstractNetexPlugin
import org.entur.ror.ubelluris.model.NetexTypes
import org.xml.sax.Attributes

/**
 * Collects the KeyValue pairs of the entities that [collectedKeysByEntityType] names, e.g. Line -> salesAuthorityNumber.
 * A KeyValue belongs to the innermost entity, so the keyList of a nested entity, e.g. ServiceFacilitySet, is not collected.
 */
class KeyValueCollectorPlugin(
    private val collectedKeysByEntityType: Map<String, Set<String>>,
) : AbstractNetexPlugin() {
    private val keyValuesByEntityId = mutableMapOf<String, MutableMap<String, String>>()
    private val elementText = StringBuilder()
    private var pendingKey: String? = null

    override fun getName(): String = javaClass.name

    override fun getDescription(): String = "Collects the KeyValue pairs of the configured entity types and keys"

    override fun getSupportedElementTypes() =
        setOf(
            "${NetexTypes.KEY_VALUE}/${NetexTypes.KEY}",
            "${NetexTypes.KEY_VALUE}/${NetexTypes.VALUE}",
        )

    override fun startElement(
        elementName: String,
        attributes: Attributes?,
        currentEntity: Entity?,
    ) {
        elementText.clear()
    }

    override fun characters(
        elementName: String,
        ch: CharArray?,
        start: Int,
        length: Int,
    ) {
        if (ch != null) {
            elementText.appendRange(ch, start, start + length)
        }
    }

    override fun endElement(
        elementName: String,
        currentEntity: Entity?,
    ) {
        val entity = currentEntity ?: return
        val collectedKeys = collectedKeysByEntityType[entity.type] ?: return
        val text = elementText.toString().trim()

        when (elementName) {
            NetexTypes.KEY -> pendingKey = text.takeIf { it in collectedKeys }
            NetexTypes.VALUE -> {
                pendingKey?.let { key -> keyValuesByEntityId.getOrPut(entity.id) { mutableMapOf() }[key] = text }
                pendingKey = null
            }
        }
    }

    fun keyValueOrNull(
        entityId: String,
        key: String,
    ): String? = keyValuesByEntityId[entityId]?.get(key)

    override fun getCollectedData(): Map<String, Map<String, String>> = keyValuesByEntityId
}

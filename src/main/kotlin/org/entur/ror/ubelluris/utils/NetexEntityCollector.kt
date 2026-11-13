package org.entur.ror.ubelluris.utils

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

/**
 * A NeTEx entity collected by [NetexEntityCollector].
 * Values are keyed by the element path relative to the entity, e.g. "TransportMode" or "dayTypes/DayTypeRef".
 * The value of an element is its ref attribute, or its trimmed text when it is a leaf with text.
 * Key/Value pairs of the direct keyList are kept separately.
 */
class CollectedNetexEntity(
    val elementName: String,
    val id: String,
) {
    private val valuesByPath = mutableMapOf<String, MutableList<String>>()
    private val keyValues = mutableMapOf<String, String>()

    internal fun addValue(
        path: String,
        value: String,
    ) {
        valuesByPath.getOrPut(path) { mutableListOf() }.add(value)
    }

    internal fun addKeyValue(
        key: String,
        value: String,
    ) {
        keyValues[key] = value
    }

    fun hasValue(path: String): Boolean = path in valuesByPath

    fun valueOrNull(path: String): String? = valuesByPath[path]?.firstOrNull()

    fun requireValue(path: String): String = valueOrNull(path) ?: error("Missing <$path> in <$elementName id=\"$id\">")

    fun values(path: String): List<String> = valuesByPath[path].orEmpty()

    fun requireValues(path: String): List<String> = values(path).ifEmpty { error("Missing <$path> in <$elementName id=\"$id\">") }

    fun keyValueOrNull(key: String): String? = keyValues[key]

    fun requireKeyValue(key: String): String = keyValueOrNull(key) ?: error("Missing KeyValue '$key' in <$elementName id=\"$id\">")
}

/**
 * Streams a NeTEx file with SAX and collects every element whose name is in [collectedEntityNames].
 * Collected entities inside another collected entity are stored as plain values of the outer entity.
 */
class NetexEntityCollector(
    private val collectedEntityNames: Set<String>,
) {
    private val saxParserFactory =
        SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }

    fun collect(xmlPath: Path): List<CollectedNetexEntity> {
        val handler = EntityCollectingHandler(collectedEntityNames)
        Files.newInputStream(xmlPath).use { inputStream ->
            saxParserFactory.newSAXParser().parse(inputStream, handler)
        }
        return handler.collectedEntities
    }

    private class EntityCollectingHandler(
        private val collectedEntityNames: Set<String>,
    ) : DefaultHandler() {
        companion object {
            private const val KEY_PATH = "keyList/KeyValue/Key"
            private const val VALUE_PATH = "keyList/KeyValue/Value"
        }

        val collectedEntities = mutableListOf<CollectedNetexEntity>()

        private var currentEntity: CollectedNetexEntity? = null

        // Each entry is the full relative path of an open element, so the current path needs no join
        private val pathInsideEntity = ArrayDeque<String>()
        private val elementText = StringBuilder()
        private var pendingKey: String? = null

        override fun startElement(
            uri: String,
            localName: String,
            qName: String,
            attributes: Attributes,
        ) {
            elementText.clear()
            val entity = currentEntity
            if (entity == null) {
                if (localName in collectedEntityNames) {
                    val id = attributes.getValue("id") ?: error("Missing attribute 'id' on <$localName>")
                    currentEntity = CollectedNetexEntity(localName, id)
                }
                return
            }
            val parentPath = pathInsideEntity.lastOrNull()
            pathInsideEntity.addLast(if (parentPath == null) localName else "$parentPath/$localName")
            attributes.getValue("ref")?.let { entity.addValue(pathInsideEntity.last(), it) }
        }

        override fun characters(
            characters: CharArray,
            start: Int,
            length: Int,
        ) {
            if (currentEntity != null) {
                elementText.appendRange(characters, start, start + length)
            }
        }

        override fun endElement(
            uri: String,
            localName: String,
            qName: String,
        ) {
            val entity = currentEntity ?: return
            if (pathInsideEntity.isEmpty()) {
                collectedEntities.add(entity)
                currentEntity = null
                return
            }
            val text = elementText.toString().trim()
            if (text.isNotEmpty()) {
                when (val path = pathInsideEntity.last()) {
                    KEY_PATH -> pendingKey = text
                    VALUE_PATH -> pendingKey?.let { entity.addKeyValue(it, text) }.also { pendingKey = null }
                    else -> entity.addValue(path, text)
                }
            }
            elementText.clear()
            pathInsideEntity.removeLast()
        }
    }
}

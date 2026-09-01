package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.sax.plugins.data.LocalEntityRegistry
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

/**
 * Handler that rewrites versionRef to version for references to locally defined entities.
 *
 * According to NeTEx specification:
 * - `version` should be used for entities defined in the same document (local references)
 * - `versionRef` should only be used for external entities (defined in other documents)
 *
 * This handler checks if a reference element has both 'ref' and 'versionRef' attributes,
 * and if the referenced entity is defined locally (tracked by LocalEntityRegistry),
 * it rewrites 'versionRef' to 'version'.
 */
class VersionRefNormalizerHandler(
    private val registry: LocalEntityRegistry,
) : XMLElementHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var rewriteCount = 0

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        if (attributes == null) {
            writer.startElement(uri, localName, qName, attributes)
            return
        }

        val ref = attributes.getValue("ref")
        val versionRef = attributes.getValue("versionRef")
        val version = attributes.getValue("version")

        // Only process if element has both 'ref' and 'versionRef' but no 'version'
        if (ref != null && versionRef != null && version == null) {
            // Check if the referenced entity is local
            if (registry.isLocalEntity(ref)) {
                // Rewrite versionRef to version
                val newAttributes = AttributesImpl()

                // Copy all attributes except versionRef
                for (i in 0 until attributes.length) {
                    val attrName = attributes.getQName(i)
                    val attrValue = attributes.getValue(i)

                    when (attrName) {
                        "versionRef" -> {
                            // Replace versionRef with version
                            newAttributes.addAttribute(
                                attributes.getURI(i),
                                "version",
                                "version",
                                attributes.getType(i),
                                attrValue,
                            )
                            rewriteCount++
                            logger.debug(
                                "Rewrote versionRef='{}' to version='{}' for local entity {} in element {}",
                                attrValue,
                                attrValue,
                                ref,
                                localName,
                            )
                        }
                        else -> {
                            // Keep other attributes as-is
                            newAttributes.addAttribute(
                                attributes.getURI(i),
                                attributes.getLocalName(i),
                                attrName,
                                attributes.getType(i),
                                attrValue,
                            )
                        }
                    }
                }

                writer.startElement(uri, localName, qName, newAttributes)
                return
            }
        }

        // No rewrite needed, pass through as-is
        writer.startElement(uri, localName, qName, attributes)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        writer.characters(ch, start, length)
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        writer.endElement(uri, localName, qName)
    }

    /**
     * Get the number of versionRef attributes that were rewritten to version.
     */
    fun getRewriteCount(): Int = rewriteCount
}

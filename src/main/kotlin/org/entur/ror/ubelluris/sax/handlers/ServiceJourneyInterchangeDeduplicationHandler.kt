package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.sax.plugins.ServiceJourneyInterchangeCollectorPlugin
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes

/**
 * Custom element handler that deduplicates ServiceJourneyInterchange elements during XML writing.
 *
 * This single handler instance is registered for the ServiceJourneyInterchange element and all its
 * child elements. When a duplicate is encountered, it sets skipDepth > 0, which causes all subsequent
 * SAX events (including child elements) to be skipped until the depth returns to 0.
 *
 * Since the same handler instance is used for all registered paths, the instance variables are
 * naturally shared across all SAX events.
 */
class ServiceJourneyInterchangeDeduplicationHandler(
    private val collectorPlugin: ServiceJourneyInterchangeCollectorPlugin,
) : XMLElementHandler {
    private val logger = LoggerFactory.getLogger(ServiceJourneyInterchangeDeduplicationHandler::class.java)
    private val writtenIds = mutableSetOf<String>()
    private var skipDepth = 0

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        // If we're already inside a skipped element, increment depth and skip this element too
        if (skipDepth > 0) {
            skipDepth++
            return
        }

        // Check if this is a ServiceJourneyInterchange element (has an id attribute and correct qName)
        val isRootInterchangeElement = qName == "ServiceJourneyInterchange" && attributes?.getValue("id") != null

        // This is a root ServiceJourneyInterchange element - check if we should skip it
        if (isRootInterchangeElement) {
            val id = attributes?.getValue("id")
            val identicalDuplicates = collectorPlugin.getIdenticalDuplicates()
            val conflictingDuplicates = collectorPlugin.getConflictingDuplicates()

            val shouldSkip =
                when {
                    // Skip all occurrences of conflicting duplicates
                    id != null && id in conflictingDuplicates -> {
                        logger.debug("Skipping conflicting duplicate ServiceJourneyInterchange: {}", id)
                        true
                    }
                    // For identical duplicates, skip if we've already written this ID
                    id != null && id in identicalDuplicates -> {
                        if (id in writtenIds) {
                            logger.debug("Skipping identical duplicate ServiceJourneyInterchange: {}", id)
                            true
                        } else {
                            writtenIds.add(id)
                            false
                        }
                    }
                    // Not a duplicate, write it
                    else -> false
                }

            if (shouldSkip) {
                skipDepth = 1 // Start skipping this element and all its children
                return
            }
        }

        // Not skipping - write the element
        writer.startElement(uri, localName, qName, attributes)
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        // If we're inside a skipped subtree, decrement depth and skip this closing tag
        if (skipDepth > 0) {
            skipDepth--
            return
        }

        writer.endElement(uri, localName, qName)
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        // Only write characters if we're not inside a skipped element
        if (skipDepth == 0) {
            writer.characters(ch, start, length)
        }
    }
}

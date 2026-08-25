package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.sax.plugins.ServiceJourneyInterchangeCollectorPlugin
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes

/**
 * Custom element handler that deduplicates ServiceJourneyInterchange elements during XML writing.
 * 
 * - If identical duplicates exist for an ID, writes only the first occurrence
 * - If conflicting duplicates exist for an ID, skips writing all occurrences
 */
class ServiceJourneyInterchangeDeduplicationHandler(
    private val collectorPlugin: ServiceJourneyInterchangeCollectorPlugin,
) : XMLElementHandler {
    private val logger = LoggerFactory.getLogger(ServiceJourneyInterchangeDeduplicationHandler::class.java)
    private val writtenIds = mutableSetOf<String>()
    private var currentId: String? = null
    private var skipCurrentElement = false

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        // Extract the id attribute
        currentId = attributes?.getValue("id")

        val identicalDuplicates = collectorPlugin.getIdenticalDuplicates()
        val conflictingDuplicates = collectorPlugin.getConflictingDuplicates()

        skipCurrentElement =
            when {
                // Skip all occurrences of conflicting duplicates
                currentId != null && currentId in conflictingDuplicates -> {
                    logger.info("Skipping conflicting duplicate ServiceJourneyInterchange: {}", currentId)
                    true
                }
                // For identical duplicates, skip if we've already written this ID
                currentId != null && currentId in identicalDuplicates -> {
                    if (currentId in writtenIds) {
                        logger.info("Skipping identical duplicate ServiceJourneyInterchange: {}", currentId)
                        true
                    } else {
                        writtenIds.add(currentId!!)
                        false
                    }
                }
                // Not a duplicate, write it
                else -> false
            }

        if (!skipCurrentElement) {
            writer.startElement(uri, localName, qName, attributes)
        }
    }

    override fun endElement(
        uri: String?,
        localName: String?,
        qName: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        if (!skipCurrentElement) {
            writer.endElement(uri, localName, qName)
        }
        skipCurrentElement = false
        currentId = null
    }

    override fun characters(
        ch: CharArray?,
        start: Int,
        length: Int,
        writer: DelegatingXMLElementWriter,
    ) {
        if (!skipCurrentElement) {
            writer.characters(ch, start, length)
        }
    }
}

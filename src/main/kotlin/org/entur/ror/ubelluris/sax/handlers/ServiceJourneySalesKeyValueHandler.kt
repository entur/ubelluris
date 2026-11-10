package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.model.SalesData
import org.entur.ror.ubelluris.sales.SalesTimetableMatcher
import org.entur.ror.ubelluris.sax.plugins.SalesMatchDataCollector
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.AttributesImpl

/**
 * Adds productCode and salesAuthorityNumber KeyValues to each ServiceJourney that matches the sales data.
 * Register it for the paths in [handledPaths]. A sales key that is already in the keyList is logged and not added.
 * The matching runs once, at the first ServiceJourney of the export. By then [salesMatchDataCollector] has collected all files.
 */
class ServiceJourneySalesKeyValueHandler(
    private val salesData: SalesData,
    private val salesMatchDataCollector: SalesMatchDataCollector,
) : XMLElementHandler {
    companion object {
        const val PRODUCT_CODE_KEY = "productCode"
        const val SALES_AUTHORITY_NUMBER_KEY = "salesAuthorityNumber"
        private const val KEY_LIST = "keyList"
        private const val SERVICE_JOURNEY_PATH =
            "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys/ServiceJourney"

        /** The ServiceJourney, its keyList, and the children that come before keyList. */
        val handledPaths: List<String> =
            listOf(SERVICE_JOURNEY_PATH, "$SERVICE_JOURNEY_PATH/$KEY_LIST") +
                SalesMatchDataCollector.ELEMENTS_BEFORE_KEY_LIST.map { "$SERVICE_JOURNEY_PATH/$it" }
    }

    private data class PendingKeyValues(
        val keyValues: List<Pair<String, String>>,
        val writeAtEndOfElement: String,
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    private val matchesByServiceJourneyId: Map<String, SalesTimetableMatcher.ServiceJourneyMatch> by lazy {
        val matcher = SalesTimetableMatcher(salesData, salesMatchDataCollector.calendar())
        salesMatchDataCollector.railServiceJourneys().associate { it.serviceJourneyId to matcher.match(it) }
    }

    private var pendingKeyValues: PendingKeyValues? = null

    /** Match counts of all rail ServiceJourneys the collector found. Runs the matching if the export has not done it. */
    val matchSummary: SalesTimetableMatcher.MatchSummary
        get() = SalesTimetableMatcher.MatchSummary.of(matchesByServiceJourneyId.values)

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        writer.startElement(uri, localName, qName, attributes)
        if (elementNameOf(localName, qName) != NetexTypes.SERVICE_JOURNEY) {
            return
        }

        pendingKeyValues = null
        val serviceJourneyId = attributes?.getValue("id") ?: return
        val salesValues = matchesByServiceJourneyId[serviceJourneyId]?.salesValues ?: return
        val keyValues = keyValuesNotInKeyList(serviceJourneyId, salesValues)
        if (keyValues.isEmpty()) {
            return
        }

        // NeTEx order: keyList comes directly after validityConditions, ValidBetween and alternativeTexts
        val writeAtEndOfElement =
            if (salesMatchDataCollector.hasKeyList(serviceJourneyId)) {
                KEY_LIST
            } else {
                salesMatchDataCollector.lastElementBeforeKeyList(serviceJourneyId)
            }
        if (writeAtEndOfElement == null) {
            writeKeyList(uri, prefixOf(qName), keyValues, writer)
        } else {
            pendingKeyValues = PendingKeyValues(keyValues, writeAtEndOfElement)
        }
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
        val elementName = elementNameOf(localName, qName)
        val pending = pendingKeyValues?.takeIf { it.writeAtEndOfElement == elementName }

        if (pending != null && elementName == KEY_LIST) {
            pending.keyValues.forEach { (key, value) -> writeKeyValue(uri, prefixOf(qName), key, value, writer) }
        }
        writer.endElement(uri, localName, qName)
        if (pending != null && elementName != KEY_LIST) {
            writeKeyList(uri, prefixOf(qName), pending.keyValues, writer)
        }
        if (pending != null) {
            pendingKeyValues = null
        }
    }

    private fun keyValuesNotInKeyList(
        serviceJourneyId: String,
        salesValues: SalesTimetableMatcher.SalesValues,
    ): List<Pair<String, String>> {
        val existingKeys = salesMatchDataCollector.keyListKeys(serviceJourneyId)
        val (alreadyInKeyList, notInKeyList) =
            listOf(PRODUCT_CODE_KEY to salesValues.productCode, SALES_AUTHORITY_NUMBER_KEY to salesValues.salesAuthorityNumber)
                .partition { (key, _) -> key in existingKeys }
        alreadyInKeyList.forEach { (key, value) ->
            logger.warn("Service journey {} already has KeyValue {}. Sales value {} is not added", serviceJourneyId, key, value)
        }
        return notInKeyList
    }

    private fun elementNameOf(
        localName: String?,
        qName: String?,
    ): String? = localName?.takeIf { it.isNotEmpty() } ?: qName?.substringAfter(':')

    private fun prefixOf(qName: String?): String? = qName?.substringBefore(':', missingDelimiterValue = "")?.takeIf { it.isNotEmpty() }

    private fun writeKeyList(
        uri: String?,
        prefix: String?,
        keyValues: List<Pair<String, String>>,
        writer: DelegatingXMLElementWriter,
    ) = writeElement(uri, prefix, KEY_LIST, writer) {
        keyValues.forEach { (key, value) -> writeKeyValue(uri, prefix, key, value, writer) }
    }

    private fun writeKeyValue(
        uri: String?,
        prefix: String?,
        key: String,
        value: String,
        writer: DelegatingXMLElementWriter,
    ) = writeElement(uri, prefix, NetexTypes.KEY_VALUE, writer) {
        writeElement(uri, prefix, NetexTypes.KEY, writer) { writer.characters(key.toCharArray(), 0, key.length) }
        writeElement(uri, prefix, NetexTypes.VALUE, writer) { writer.characters(value.toCharArray(), 0, value.length) }
    }

    private fun writeElement(
        uri: String?,
        prefix: String?,
        localName: String,
        writer: DelegatingXMLElementWriter,
        writeContent: () -> Unit,
    ) {
        val qName = if (prefix == null) localName else "$prefix:$localName"
        writer.startElement(uri, localName, qName, AttributesImpl())
        writeContent()
        writer.endElement(uri, localName, qName)
    }
}

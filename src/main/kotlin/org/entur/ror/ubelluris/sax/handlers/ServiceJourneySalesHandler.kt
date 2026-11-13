package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sales.SalesTimetableMatcher
import org.entur.ror.ubelluris.sax.plugins.SalesMatchDataCollector
import org.entur.ror.ubelluris.sax.plugins.SalesMatchRepository
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes

/**
 * Writes the sales enrichment in [salesMatchRepository] of each converted ServiceJourney:
 * - adds productCode and salesAuthorityNumber KeyValues to its keyList. A sales key that is already in the keyList is logged and not added.
 * - leaves out its dayTypes,
 * - writes one DatedServiceJourney per operating date directly after it, in the same vehicleJourneys.
 */
class ServiceJourneySalesHandler(
    private val salesMatchRepository: SalesMatchRepository,
) : XMLElementHandler {
    companion object {
        const val PRODUCT_CODE_KEY = "productCode"
        const val SALES_AUTHORITY_NUMBER_KEY = "salesAuthorityNumber"
        private const val KEY_LIST = "keyList"
        private const val DAY_TYPES = "dayTypes"
        private const val DAY_TYPE_REF = "DayTypeRef"
        private const val SERVICE_JOURNEY_PATH =
            "/PublicationDelivery/dataObjects/CompositeFrame/frames/TimetableFrame/vehicleJourneys/ServiceJourney"

        /** The ServiceJourney, its keyList, the children that come before keyList, and its dayTypes. */
        val handledPaths: List<String> =
            listOf(
                SERVICE_JOURNEY_PATH,
                "$SERVICE_JOURNEY_PATH/$KEY_LIST",
                "$SERVICE_JOURNEY_PATH/$DAY_TYPES",
                "$SERVICE_JOURNEY_PATH/$DAY_TYPES/$DAY_TYPE_REF",
            ) + SalesMatchDataCollector.ELEMENTS_BEFORE_KEY_LIST.map { "$SERVICE_JOURNEY_PATH/$it" }
    }

    /**
     * KeyValues still to write for the ServiceJourney that is being written.
     * They go inside [writeAtEndOfElement] when it is the keyList, else in a new keyList after it.
     */
    private data class PendingKeyValues(
        val keyValues: List<Pair<String, String>>,
        val writeAtEndOfElement: String,
    )

    /** The converted ServiceJourney that is being written. */
    private data class ConvertedServiceJourney(
        val id: String,
        val version: String?,
    )

    private val logger = LoggerFactory.getLogger(javaClass)

    private var pendingKeyValues: PendingKeyValues? = null
    private var convertedServiceJourney: ConvertedServiceJourney? = null

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        val elementName = elementNameOf(localName, qName)
        if (dropDayTypesOfConvertedSJ(elementName)) {
            return
        }
        writer.startElement(uri, localName, qName, attributes)
        if (elementName != NetexTypes.SERVICE_JOURNEY) {
            return
        }

        pendingKeyValues = null
        convertedServiceJourney = null
        val serviceJourneyId = attributes?.getValue("id") ?: return
        if (salesMatchRepository.salesEnrichment.isConverted(serviceJourneyId)) {
            convertedServiceJourney = ConvertedServiceJourney(serviceJourneyId, attributes.getValue("version"))
        }
        startKeyValues(uri, qName, serviceJourneyId, writer)
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
        if (dropDayTypesOfConvertedSJ(elementName)) {
            return
        }
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
        if (elementName == NetexTypes.SERVICE_JOURNEY) {
            convertedServiceJourney?.let { writeDatedServiceJourneys(uri, prefixOf(qName), it, writer) }
            convertedServiceJourney = null
        }
    }

    // The dayTypes of a converted ServiceJourney are replaced by its DatedServiceJourneys
    private fun dropDayTypesOfConvertedSJ(elementName: String?): Boolean =
        convertedServiceJourney != null && (elementName == DAY_TYPES || elementName == DAY_TYPE_REF)

    private fun startKeyValues(
        uri: String?,
        qName: String?,
        serviceJourneyId: String,
        writer: DelegatingXMLElementWriter,
    ) {
        val salesValues = salesMatchRepository.salesEnrichment.salesValues(serviceJourneyId) ?: return
        val keyValues = keyValuesNotInKeyList(serviceJourneyId, salesValues)
        if (keyValues.isEmpty()) {
            return
        }

        // NeTEx order: keyList comes directly after validityConditions, ValidBetween and alternativeTexts
        val writeAtEndOfElement =
            if (salesMatchRepository.hasKeyList(serviceJourneyId)) {
                KEY_LIST
            } else {
                salesMatchRepository.lastElementBeforeKeyList(serviceJourneyId)
            }
        if (writeAtEndOfElement == null) {
            writeKeyList(uri, prefixOf(qName), keyValues, writer)
        } else {
            pendingKeyValues = PendingKeyValues(keyValues, writeAtEndOfElement)
        }
    }

    private fun keyValuesNotInKeyList(
        serviceJourneyId: String,
        salesValues: SalesTimetableMatcher.SalesValues,
    ): List<Pair<String, String>> {
        val existingKeys = salesMatchRepository.keyListKeys(serviceJourneyId)
        val (alreadyInKeyList, notInKeyList) =
            listOf(PRODUCT_CODE_KEY to salesValues.productCode, SALES_AUTHORITY_NUMBER_KEY to salesValues.salesAuthorityNumber)
                .partition { (key, _) -> key in existingKeys }
        alreadyInKeyList.forEach { (key, value) ->
            logger.warn("Service journey {} already has KeyValue {}. Sales value {} is not added", serviceJourneyId, key, value)
        }
        return notInKeyList
    }

    private fun writeKeyList(
        uri: String?,
        prefix: String?,
        keyValues: List<Pair<String, String>>,
        writer: DelegatingXMLElementWriter,
    ) = writer.writeElement(uri, prefix, KEY_LIST) {
        keyValues.forEach { (key, value) -> writeKeyValue(uri, prefix, key, value, writer) }
    }

    private fun writeKeyValue(
        uri: String?,
        prefix: String?,
        key: String,
        value: String,
        writer: DelegatingXMLElementWriter,
    ) = writer.writeElement(uri, prefix, NetexTypes.KEY_VALUE) {
        writer.writeElement(uri, prefix, NetexTypes.KEY) { writer.writeText(key) }
        writer.writeElement(uri, prefix, NetexTypes.VALUE) { writer.writeText(value) }
    }

    // NeTEx order: ServiceJourneyRef comes before OperatingDayRef
    private fun writeDatedServiceJourneys(
        uri: String?,
        prefix: String?,
        serviceJourney: ConvertedServiceJourney,
        writer: DelegatingXMLElementWriter,
    ) {
        val versionAttribute = serviceJourney.version?.let { "version" to it }
        salesMatchRepository.salesEnrichment.datedServiceJourneys(serviceJourney.id).forEach { datedServiceJourney ->
            writer.writeElement(
                uri,
                prefix,
                "DatedServiceJourney",
                attributesOf(*listOfNotNull(versionAttribute, "id" to datedServiceJourney.datedServiceJourneyId).toTypedArray()),
            ) {
                writer.writeElement(
                    uri,
                    prefix,
                    "ServiceJourneyRef",
                    attributesOf(*listOfNotNull("ref" to serviceJourney.id, versionAttribute).toTypedArray()),
                )
                writer.writeElement(uri, prefix, "OperatingDayRef", attributesOf("ref" to datedServiceJourney.operatingDayId))
            }
        }
    }
}

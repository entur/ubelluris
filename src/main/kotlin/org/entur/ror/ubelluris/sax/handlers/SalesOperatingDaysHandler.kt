package org.entur.ror.ubelluris.sax.handlers

import org.entur.netex.tools.lib.output.DelegatingXMLElementWriter
import org.entur.netex.tools.lib.output.XMLElementHandler
import org.entur.ror.ubelluris.sales.SalesEnrichmentPlan
import org.xml.sax.Attributes

/**
 * Writes the OperatingDays that the DatedServiceJourneys of [salesEnrichmentPlan] refer to into _shared_data.xml.
 */
class SalesOperatingDaysHandler(
    private val salesEnrichmentPlan: SalesEnrichmentPlan,
) : XMLElementHandler {
    companion object {
        private const val SERVICE_CALENDAR_FRAME = "ServiceCalendarFrame"
        private const val SERVICE_CALENDAR_FRAME_PATH = "/PublicationDelivery/dataObjects/CompositeFrame/frames/$SERVICE_CALENDAR_FRAME"

        // The children of a ServiceCalendarFrame that come after operatingDays
        private val ELEMENTS_AFTER_OPERATING_DAYS = setOf("operatingPeriods", "dayTypeAssignments")

        val handledPaths: List<String> =
            listOf(SERVICE_CALENDAR_FRAME_PATH) + ELEMENTS_AFTER_OPERATING_DAYS.map { "$SERVICE_CALENDAR_FRAME_PATH/$it" }
    }

    private var operatingDaysWritten = false

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?,
        writer: DelegatingXMLElementWriter,
    ) {
        if (elementNameOf(localName, qName) in ELEMENTS_AFTER_OPERATING_DAYS) {
            writeOperatingDaysOnce(uri, prefixOf(qName), writer)
        }
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
        if (elementNameOf(localName, qName) == SERVICE_CALENDAR_FRAME) {
            writeOperatingDaysOnce(uri, prefixOf(qName), writer)
        }
        writer.endElement(uri, localName, qName)
    }

    private fun writeOperatingDaysOnce(
        uri: String?,
        prefix: String?,
        writer: DelegatingXMLElementWriter,
    ) {
        if (operatingDaysWritten) {
            return
        }
        operatingDaysWritten = true
        val operatingDays = salesEnrichmentPlan.operatingDays
        if (operatingDays.isEmpty()) {
            return
        }
        writer.writeElement(uri, prefix, "operatingDays") {
            operatingDays.forEach { operatingDay ->
                writer.writeElement(
                    uri,
                    prefix,
                    "OperatingDay",
                    attributesOf("version" to SalesEnrichmentPlan.OPERATING_DAY_VERSION, "id" to operatingDay.operatingDayId),
                ) {
                    writer.writeElement(uri, prefix, "CalendarDate") { writer.writeText(operatingDay.date.toString()) }
                }
            }
        }
    }
}

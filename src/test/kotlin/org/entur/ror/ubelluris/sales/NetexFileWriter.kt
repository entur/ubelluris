package org.entur.ror.ubelluris.sales

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Marshaller
import org.rutebanken.netex.model.Common_VersionFrameStructure
import org.rutebanken.netex.model.CompositeFrame
import org.rutebanken.netex.model.Frames_RelStructure
import org.rutebanken.netex.model.ObjectFactory
import org.rutebanken.netex.model.PublicationDeliveryStructure
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime

/** The version of every NeTEx entity in the test files. */
const val TEST_NETEX_VERSION = "1"

/** Wraps NeTEx members in the JAXBElements that their containers need, e.g. frames, Lines and refs. */
val netexFactory = ObjectFactory()

private val publicationDeliveryContext: JAXBContext by lazy { JAXBContext.newInstance(PublicationDeliveryStructure::class.java) }

/**
 * Writes a Samtrafiken-like PublicationDelivery with one CompositeFrame that holds [frames] to [filePath].
 * JAXB writes the elements in NeTEx schema order, with NeTEx as the default namespace.
 */
fun writePublicationDelivery(
    filePath: Path,
    compositeFrameId: String,
    vararg frames: JAXBElement<out Common_VersionFrameStructure>,
) {
    val publicationDelivery =
        PublicationDeliveryStructure()
            .withVersion("1.15:NO-NeTEx-networktimetable:1.5")
            .withPublicationTimestamp(LocalDateTime.parse("2026-11-03T21:30:59"))
            .withParticipantRef("SAM")
            .withDataObjects(
                PublicationDeliveryStructure.DataObjects().withCompositeFrameOrCommonFrame(
                    netexFactory.createCompositeFrame(
                        CompositeFrame()
                            .withId(compositeFrameId)
                            .withVersion(TEST_NETEX_VERSION)
                            .withFrames(Frames_RelStructure().withCommonFrame(*frames)),
                    ),
                ),
            )

    Files.newOutputStream(filePath).use { outputStream ->
        publicationDeliveryContext
            .createMarshaller()
            .apply { setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true) }
            .marshal(netexFactory.createPublicationDelivery(publicationDelivery), outputStream)
    }
}

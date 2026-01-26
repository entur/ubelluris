package org.entur.ror.ubelluris.sax.plugins

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.data.TestDataFactory.defaultEntity
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.data.QuayData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes
import java.nio.file.Path

class StopPlacePurgingPluginTest {

    @TempDir
    lateinit var tempDir: Path

    private fun createPlugin(blacklistContent: String = ""): StopPlacePurgingPlugin {
        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText(blacklistContent)
        return StopPlacePurgingPlugin(
            StopPlacePurgingRepository(),
            blacklistFile.absolutePath
        )
    }

    @Test
    fun shouldReturnSupportedElementTypes() {
        val plugin = createPlugin()

        val supportedTypes = plugin.getSupportedElementTypes()

        assertThat(supportedTypes).containsExactlyInAnyOrder(
            NetexTypes.PUBLIC_CODE,
            NetexTypes.QUAY,
            NetexTypes.PARENT_SITE_REF
        )
    }

    @Test
    fun shouldReturnRepositoryAsCollectedData() {
        val repository = StopPlacePurgingRepository()
        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText("")
        val plugin = StopPlacePurgingPlugin(repository, blacklistFile.absolutePath)

        assertThat(plugin.getCollectedData()).isSameAs(repository)
    }

    @Test
    fun shouldDelegatePublicCodeStartElementToHandler() {
        val plugin = createPlugin()
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        plugin.startElement(NetexTypes.PUBLIC_CODE, null, quayEntity)
        plugin.characters(NetexTypes.PUBLIC_CODE, "A".toCharArray(), 0, 1)
        plugin.endElement(NetexTypes.PUBLIC_CODE, quayEntity)

        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace)
            .containsKey("stopPlaceId")
        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace["stopPlaceId"])
            .containsExactly(QuayData("quayId", "A"))
    }

    @Test
    fun shouldDelegateQuayStartElementToHandlerAndSetContext() {
        val plugin = createPlugin()
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        plugin.startElement(NetexTypes.QUAY, null, quayEntity)
        plugin.endElement(NetexTypes.QUAY, quayEntity)

        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace)
            .containsKey("stopPlaceId")
        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace["stopPlaceId"])
            .containsExactly(QuayData("quayId", ""))
    }

    @Test
    fun shouldCallBlacklistQuayHandlerWhenProcessingQuayElement() {
        val plugin = createPlugin("SE:050:Quay:12345")
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:Quay:12345")

        plugin.startElement(NetexTypes.QUAY, attrs, quayEntity)

        assertThat(plugin.stopPlacePurgingRepository.entityIds).containsOnly("quayId")
    }

    @Test
    fun shouldNotCallBlacklistQuayHandlerForNonQuayElements() {
        val plugin = createPlugin("SE:050:Quay:12345")
        val entity = defaultEntity(id = "entityId", type = NetexTypes.PUBLIC_CODE)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:Quay:12345")

        plugin.startElement(NetexTypes.PUBLIC_CODE, attrs, entity)

        assertThat(plugin.stopPlacePurgingRepository.entityIds).isEmpty()
    }

    @Test
    fun shouldDelegateParentSiteRefToHandler() {
        val plugin = createPlugin()
        val childStopPlace = defaultEntity(id = "childStopPlaceId", type = NetexTypes.STOP_PLACE)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("ref")).thenReturn("parentStopPlaceId")

        plugin.startElement(NetexTypes.PARENT_SITE_REF, attrs, childStopPlace)
        plugin.endElement(NetexTypes.PARENT_SITE_REF, childStopPlace)

        assertThat(plugin.stopPlacePurgingRepository.parentSiteRefsPerStopPlace)
            .containsKey("parentStopPlaceId")
        assertThat(plugin.stopPlacePurgingRepository.parentSiteRefsPerStopPlace["parentStopPlaceId"])
            .containsOnly("childStopPlaceId")
        assertThat(plugin.stopPlacePurgingRepository.childStopPlaces)
            .containsOnly("childStopPlaceId")
    }

    @Test
    fun shouldNotProcessStartElementWhenCurrentEntityIsNull() {
        val plugin = createPlugin()

        plugin.startElement(NetexTypes.PUBLIC_CODE, null, null)

        assertThat(plugin.stopPlacePurgingRepository.entityIds).isEmpty()
        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace).isEmpty()
    }

    @Test
    fun shouldNotProcessEndElementWhenCurrentEntityIsNull() {
        val plugin = createPlugin()
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY)

        plugin.startElement(NetexTypes.PUBLIC_CODE, null, quayEntity)
        plugin.characters(NetexTypes.PUBLIC_CODE, "A".toCharArray(), 0, 1)
        plugin.endElement(NetexTypes.PUBLIC_CODE, null)

        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace).isEmpty()
    }

    @Test
    fun shouldIgnoreUnsupportedElementTypes() {
        val plugin = createPlugin()
        val entity = defaultEntity(id = "entityId", type = "UnsupportedType")

        plugin.startElement("UnsupportedElement", null, entity)
        plugin.characters("UnsupportedElement", "test".toCharArray(), 0, 4)
        plugin.endElement("UnsupportedElement", entity)

        assertThat(plugin.stopPlacePurgingRepository.entityIds).isEmpty()
        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace).isEmpty()
    }

    @Test
    fun shouldNotAddQuayToStopPlaceWhenQuayHasPublicCode() {
        val plugin = createPlugin()
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity = defaultEntity(id = "quayId", type = NetexTypes.QUAY, parent = parentEntity)

        plugin.startElement(NetexTypes.QUAY, null, quayEntity)
        plugin.startElement(NetexTypes.PUBLIC_CODE, null, quayEntity)
        plugin.characters(NetexTypes.PUBLIC_CODE, "A".toCharArray(), 0, 1)
        plugin.endElement(NetexTypes.PUBLIC_CODE, quayEntity)
        plugin.endElement(NetexTypes.QUAY, quayEntity)

        val quays = plugin.stopPlacePurgingRepository.quaysPerStopPlace["stopPlaceId"]
        assertThat(quays).hasSize(1)
        assertThat(quays).containsExactly(QuayData("quayId", "A"))
    }

    @Test
    fun shouldProcessMultipleQuaysForSameStopPlace() {
        val plugin = createPlugin()
        val parentEntity = defaultEntity(id = "stopPlaceId", type = NetexTypes.STOP_PLACE)
        val quayEntity1 = defaultEntity(id = "quayId1", type = NetexTypes.QUAY, parent = parentEntity)
        val quayEntity2 = defaultEntity(id = "quayId2", type = NetexTypes.QUAY, parent = parentEntity)

        plugin.startElement(NetexTypes.QUAY, null, quayEntity1)
        plugin.startElement(NetexTypes.PUBLIC_CODE, null, quayEntity1)
        plugin.characters(NetexTypes.PUBLIC_CODE, "A".toCharArray(), 0, 1)
        plugin.endElement(NetexTypes.PUBLIC_CODE, quayEntity1)
        plugin.endElement(NetexTypes.QUAY, quayEntity1)

        plugin.startElement(NetexTypes.QUAY, null, quayEntity2)
        plugin.startElement(NetexTypes.PUBLIC_CODE, null, quayEntity2)
        plugin.characters(NetexTypes.PUBLIC_CODE, "B".toCharArray(), 0, 1)
        plugin.endElement(NetexTypes.PUBLIC_CODE, quayEntity2)
        plugin.endElement(NetexTypes.QUAY, quayEntity2)

        assertThat(plugin.stopPlacePurgingRepository.quaysPerStopPlace["stopPlaceId"])
            .containsExactlyInAnyOrder(
                QuayData("quayId1", "A"),
                QuayData("quayId2", "B")
            )
    }
}
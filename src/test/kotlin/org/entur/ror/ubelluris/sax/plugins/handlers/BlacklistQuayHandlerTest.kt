package org.entur.ror.ubelluris.sax.plugins.handlers

import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.data.TestDataFactory.defaultEntity
import org.entur.ror.ubelluris.model.NetexTypes
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingParsingContext
import org.entur.ror.ubelluris.sax.plugins.StopPlacePurgingRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.xml.sax.Attributes
import java.io.File
import java.nio.file.Path

class BlacklistQuayHandlerTest {
    private val context = StopPlacePurgingParsingContext()

    private val stopPlacePurgingRepository = StopPlacePurgingRepository()

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldAddEntityIdWhenQuayIdIsInBlacklist() {

        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText(
            """
            SE:050:Quay:47296
            SE:050:Quay:47297
            SE:050:Quay:47298
            """.trimIndent()
        )

        val handler = BlacklistQuayHandler(stopPlacePurgingRepository, blacklistFile)
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:Quay:47296")

        handler.startElement(context, attrs, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).containsOnly("entityId")
    }

    @Test
    fun shouldNotAddEntityIdWhenQuayIdIsNotInBlacklist() {
        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText(
            """
            SE:050:Quay:47296
            SE:050:Quay:47297
            """.trimIndent()
        )

        val handler = BlacklistQuayHandler(stopPlacePurgingRepository, blacklistFile)
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:Quay:99999")

        handler.startElement(context, attrs, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).isEmpty()
    }

    @Test
    fun shouldNotAddEntityIdWhenAttributesAreNull() {
        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText("SE:050:Quay:47296")

        val handler = BlacklistQuayHandler(stopPlacePurgingRepository, blacklistFile)
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        handler.startElement(context, null, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).isEmpty()
    }

    @Test
    fun shouldNotAddEntityIdWhenIdAttributeIsNull() {
        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText("SE:050:Quay:47296")

        val handler = BlacklistQuayHandler(stopPlacePurgingRepository, blacklistFile)
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn(null)

        handler.startElement(context, attrs, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).isEmpty()
    }

    @Test
    fun shouldHandleEmptyBlacklistFile() {
        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText("")

        val handler = BlacklistQuayHandler(stopPlacePurgingRepository, blacklistFile)
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:Quay:47296")

        handler.startElement(context, attrs, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).isEmpty()
    }

    @Test
    fun shouldHandleNonExistentBlacklistFile() {
        val blacklistFile = File(tempDir.resolve("nonexistent.txt").toString())

        val handler = BlacklistQuayHandler(stopPlacePurgingRepository, blacklistFile)
        val quayEntity = defaultEntity(id = "entityId", type = NetexTypes.QUAY)

        val attrs: Attributes = mock()
        whenever(attrs.getValue("id")).thenReturn("SE:050:Quay:47296")

        handler.startElement(context, attrs, quayEntity)

        assertThat(stopPlacePurgingRepository.entityIds).isEmpty()
    }

    @Test
    fun shouldIgnoreEmptyLinesAndWhitespace() {
        val blacklistFile = tempDir.resolve("blacklist.txt").toFile()
        blacklistFile.writeText(
            """

            SE:050:Quay:47296

            SE:050:Quay:47297

            """.trimIndent()
        )

        val handler = BlacklistQuayHandler(stopPlacePurgingRepository, blacklistFile)
        val quayEntity1 = defaultEntity(id = "entityId1", type = NetexTypes.QUAY)
        val quayEntity2 = defaultEntity(id = "entityId2", type = NetexTypes.QUAY)

        val attrs1: Attributes = mock()
        whenever(attrs1.getValue("id")).thenReturn("SE:050:Quay:47296")

        val attrs2: Attributes = mock()
        whenever(attrs2.getValue("id")).thenReturn("SE:050:Quay:47297")

        handler.startElement(context, attrs1, quayEntity1)
        handler.startElement(context, attrs2, quayEntity2)

        assertThat(stopPlacePurgingRepository.entityIds).containsOnly("entityId1", "entityId2")
    }

}
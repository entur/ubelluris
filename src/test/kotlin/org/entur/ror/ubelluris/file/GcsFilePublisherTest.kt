package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.config.GcsConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class GcsFilePublisherTest {
    private val config =
        GcsConfig(
            "test-project",
            "test-bucket",
            "test-input-bucket",
            true,
        )

    private val mockStorage: Storage = mock()
    private val mockBlob: Blob = mock()
    private val storagePath = Path.of("2026", "01", "01")

    private val filePublisher = GcsFilePublisher(config, mockStorage, storagePath)

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldPublishFile() {
        val xmlFile = tempDir.resolve(storagePath).resolve("file_to_publish.xml")
        Files.createDirectories(xmlFile.parent)

        Files.writeString(
            xmlFile,
            """
            <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
              <stopPlaces>
                <StopPlace id="SAM:StopPlace:1000">
                  <quays>
                    <Quay id="SAM:Quay:50001"/>
                  </quays>
                </StopPlace>
              </stopPlaces>
            </PublicationDelivery>
            """.trimIndent(),
        )

        whenever(mockStorage.createFrom(any<BlobInfo>(), any<InputStream>())).thenReturn(mockBlob)

        val result = filePublisher.publish(xmlFile, emptyMap())

        assertThat(result).isEqualTo(Path.of("test-bucket/2026/01/01/stops/file_to_publish.xml"))
    }

    @Test
    fun shouldUploadToBucketWithCorrectBlobInfo() {
        val xmlFile = tempDir.resolve("test_file.xml")
        Files.writeString(xmlFile, "<test>content</test>")

        whenever(mockStorage.createFrom(any<BlobInfo>(), any<InputStream>())).thenReturn(mockBlob)

        filePublisher.publish(xmlFile, emptyMap())

        val blobInfoCaptor = argumentCaptor<BlobInfo>()
        verify(mockStorage).createFrom(blobInfoCaptor.capture(), any<InputStream>())

        val capturedBlobInfo = blobInfoCaptor.firstValue
        assertThat(capturedBlobInfo.bucket).isEqualTo("test-bucket")
        assertThat(capturedBlobInfo.name).isEqualTo("2026/01/01/stops/test_file.xml")
    }

    @Test
    fun shouldUploadTimetableFilesToBucket() {
        val stopPlaceFile = tempDir.resolve("stops.xml")
        Files.writeString(stopPlaceFile, "<StopPlaces/>")

        val providers = listOf("RUT", "ATB")
        val timetablePaths =
            providers.associate { provider ->
                val dir = tempDir.resolve("timetable").resolve(provider)
                Files.createDirectories(dir)
                Files.writeString(dir.resolve("${provider}_line_001.xml"), "<Line />")
                provider to dir
            }

        whenever(mockStorage.createFrom(any<BlobInfo>(), any<InputStream>())).thenReturn(mockBlob)

        filePublisher.publish(stopPlaceFile, timetablePaths)

        val blobInfoCaptor = argumentCaptor<BlobInfo>()
        verify(mockStorage, times(3)).createFrom(blobInfoCaptor.capture(), any<InputStream>())

        val blobNames = blobInfoCaptor.allValues.map { it.name }
        assertThat(blobNames).contains("2026/01/01/stops/stops.xml")
        providers.forEach { provider ->
            assertThat(blobNames).contains("2026/01/01/timetable/$provider/${provider}_line_001.xml")
        }
    }
}

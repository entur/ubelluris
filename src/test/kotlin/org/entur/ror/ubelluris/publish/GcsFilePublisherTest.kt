package org.entur.ror.ubelluris.publish

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.assertj.core.api.Assertions.assertThat
import org.entur.ror.ubelluris.config.GcsConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

class GcsFilePublisherTest {

    private val config = GcsConfig(
        "test-project",
        "test-bucket",
        true
    )

    private val mockStorage: Storage = mock()
    private val mockBlob: Blob = mock()

    private val filePublisher = GcsFilePublisher(config, mockStorage)

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun shouldPublishFile() {
        val xmlFile = tempDir.resolve("file_to_publish.xml")

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
        """.trimIndent()
        )

        whenever(mockStorage.createFrom(any<BlobInfo>(), any<InputStream>())).thenReturn(mockBlob)

        val result = filePublisher.publish(xmlFile)

        assertThat(result).isEqualTo(Path.of("test-bucket/file_to_publish.xml"))
    }

    @Test
    fun shouldUploadToBucketWithCorrectBlobInfo() {
        val xmlFile = tempDir.resolve("test_file.xml")
        Files.writeString(xmlFile, "<test>content</test>")

        whenever(mockStorage.createFrom(any<BlobInfo>(), any<InputStream>())).thenReturn(mockBlob)

        filePublisher.publish(xmlFile)

        val blobInfoCaptor = argumentCaptor<BlobInfo>()
        verify(mockStorage).createFrom(blobInfoCaptor.capture(), any<InputStream>())

        val capturedBlobInfo = blobInfoCaptor.firstValue
        assertThat(capturedBlobInfo.bucket).isEqualTo("test-bucket")
        assertThat(capturedBlobInfo.name).isEqualTo("test_file.xml")
    }
}
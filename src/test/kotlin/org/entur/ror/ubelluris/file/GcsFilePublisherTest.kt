package org.entur.ror.ubelluris.file

import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.transfermanager.ParallelUploadConfig
import com.google.cloud.storage.transfermanager.TransferManager
import com.google.cloud.storage.transfermanager.TransferStatus
import com.google.cloud.storage.transfermanager.UploadJob
import com.google.cloud.storage.transfermanager.UploadResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.entur.ror.ubelluris.config.GcsConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path

class GcsFilePublisherTest {
    private val config =
        GcsConfig(
            "test-project",
            "test-bucket",
            "test-input-bucket",
            true,
            true,
        )

    private val mockStorage: Storage = mock()
    private val mockTransferManager: TransferManager = mock()
    private val storagePath = Path.of("2026", "01", "01")

    private val filePublisher = GcsFilePublisher(config, mockStorage, storagePath) { mockTransferManager }

    private fun stubSuccessfulUploads() {
        whenever(mockTransferManager.uploadFiles(any(), any())).thenAnswer { invocation ->
            val files = invocation.getArgument<List<Path>>(0)
            val uploadConfig = invocation.getArgument<ParallelUploadConfig>(1)
            val results =
                files.map { file ->
                    val blobInfo = uploadConfig.uploadBlobInfoFactory.apply(uploadConfig.bucketName, file.toString())
                    UploadResult.newBuilder(blobInfo, TransferStatus.SUCCESS).setUploadedBlob(blobInfo).build()
                }
            mock<UploadJob> { on { uploadResults } doReturn results }
        }
    }

    private fun uploadedBlobs(): List<BlobInfo> {
        val filesCaptor = argumentCaptor<List<Path>>()
        val configCaptor = argumentCaptor<ParallelUploadConfig>()
        verify(mockTransferManager).uploadFiles(filesCaptor.capture(), configCaptor.capture())
        val uploadConfig = configCaptor.firstValue
        return filesCaptor.firstValue.map { uploadConfig.uploadBlobInfoFactory.apply(uploadConfig.bucketName, it.toString()) }
    }

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

        stubSuccessfulUploads()

        val result = filePublisher.publish(xmlFile, emptyMap())

        assertThat(result).isEqualTo(Path.of("test-bucket/2026/01/01/stops/file_to_publish.xml"))
    }

    @Test
    fun shouldUploadToBucketWithCorrectBlobInfo() {
        val xmlFile = tempDir.resolve("test_file.xml")
        Files.writeString(xmlFile, "<test>content</test>")

        stubSuccessfulUploads()

        filePublisher.publish(xmlFile, emptyMap())

        val capturedBlobInfo = uploadedBlobs().single()
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

        stubSuccessfulUploads()

        filePublisher.publish(stopPlaceFile, timetablePaths)

        val blobNames = uploadedBlobs().map { it.name }
        assertThat(blobNames).hasSize(3)
        assertThat(blobNames).contains("2026/01/01/stops/stops.xml")
        providers.forEach { provider ->
            assertThat(blobNames).contains("2026/01/01/timetable/$provider.zip")
        }
    }

    @Test
    fun shouldFailWhenUploadDoesNotSucceed() {
        val xmlFile = tempDir.resolve("test_file.xml")
        Files.writeString(xmlFile, "<test>content</test>")

        val failedResult =
            UploadResult
                .newBuilder(BlobInfo.newBuilder("test-bucket", "test_file.xml").build(), TransferStatus.FAILED_TO_FINISH)
                .setException(RuntimeException("Connection reset"))
                .build()
        val failedJob = mock<UploadJob> { on { uploadResults } doReturn listOf(failedResult) }
        whenever(mockTransferManager.uploadFiles(any(), any())).thenReturn(failedJob)

        assertThatThrownBy { filePublisher.publish(xmlFile, emptyMap()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("FAILED_TO_FINISH")
    }
}

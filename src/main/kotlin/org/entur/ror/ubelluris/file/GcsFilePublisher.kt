package org.entur.ror.ubelluris.file

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.transfermanager.ParallelUploadConfig
import com.google.cloud.storage.transfermanager.TransferManager
import com.google.cloud.storage.transfermanager.TransferManagerConfig
import com.google.cloud.storage.transfermanager.TransferStatus
import com.google.cloud.storage.transfermanager.UploadJob
import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.utils.LogKeys.PROVIDER
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.measureTime

class GcsFilePublisher(
    private val config: GcsConfig,
    storage: Storage,
    private val storagePath: Path,
    private val transferManagerFactory: () -> TransferManager = {
        TransferManagerConfig
            .newBuilder()
            .setStorageOptions(storage.options)
            .build()
            .service
    },
) : FilePublisher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(
        stopPlacePath: Path,
        timetablePaths: Map<String, Path>,
    ): Path {
        val stopBlobName = storagePath.resolve(FilePublisher.STOPS_DIR).resolve(stopPlacePath.fileName)
        val zipDir = Files.createTempDirectory(ZIP_DIR_PREFIX)

        try {
            val zipFiles = zipTimetables(timetablePaths, zipDir)

            val timetableBlobNames =
                zipFiles.values.associateWith { zipFile ->
                    storagePath.resolve(FilePublisher.TIMETABLE_DIR).resolve(zipFile.fileName)
                }
            upload(mapOf(stopPlacePath to stopBlobName) + timetableBlobNames)
        } finally {
            timetablePaths.keys.forEach { provider -> Files.deleteIfExists(zipDir.resolve(zipFileName(provider))) }
            Files.deleteIfExists(zipDir)
        }

        logger.info("Successfully uploaded filtered files to Ubelluris bucket.")
        return Path.of(config.outputBucketName).resolve(stopBlobName)
    }

    private fun zipTimetables(
        timetablePaths: Map<String, Path>,
        zipDir: Path,
    ): Map<String, Path> =
        timetablePaths
            .mapValues { (provider, timetablePath) ->
                zipTimetable(provider, timetablePath, zipDir)
            }

    private fun zipTimetable(
        provider: String,
        timetablePath: Path,
        zipDir: Path,
    ): Path =
        MDC.putCloseable(PROVIDER, provider).use {
            val zipFile = zipDir.resolve(zipFileName(provider))
            val zipTime = measureTime { zipDirectory(timetablePath, zipFile) }
            logger.info("Zipped timetable for provider: {} in {}", kv(PROVIDER, provider), zipTime)
            zipFile
        }

    private fun upload(blobNames: Map<Path, Path>) {
        // Transfer Manager resolves files to absolute paths before asking for blob names
        val blobNamesByFile = blobNames.entries.associate { (file, blobName) -> file.absoluteNormalized() to blobName.joinToString("/") }
        val uploadConfig =
            ParallelUploadConfig
                .newBuilder()
                .setBucketName(config.outputBucketName)
                .setUploadBlobInfoFactory { bucketName, fileName ->
                    BlobInfo.newBuilder(BlobId.of(bucketName, blobNamesByFile.getValue(Path.of(fileName).absoluteNormalized()))).build()
                }.build()

        logger.info("Uploading files: {}", blobNamesByFile.values)
        val uploadTime =
            measureTime {
                transferManagerFactory().use { transferManager ->
                    awaitSuccess(transferManager.uploadFiles(blobNames.keys.toList(), uploadConfig))
                }
            }
        logger.info("Uploaded {} files in {}", blobNames.size, uploadTime)
    }

    private fun awaitSuccess(job: UploadJob) {
        job.uploadResults.forEach { result ->
            if (result.status != TransferStatus.SUCCESS) {
                throw IllegalStateException(
                    "Upload of ${result.input.name} failed with status ${result.status}",
                    result.exception,
                )
            }
        }
    }

    private fun Path.absoluteNormalized(): Path = toAbsolutePath().normalize()

    private fun zipFileName(provider: String): String = "$provider$ZIP_SUFFIX"

    private companion object {
        const val ZIP_DIR_PREFIX = "ubelluris-timetables"
        const val ZIP_SUFFIX = ".zip"
    }
}

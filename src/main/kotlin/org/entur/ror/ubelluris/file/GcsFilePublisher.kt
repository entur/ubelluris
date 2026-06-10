package org.entur.ror.ubelluris.file

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import net.logstash.logback.argument.StructuredArguments.kv
import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.utils.LogKeys.PROVIDER
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.nio.file.Files
import java.nio.file.Path

class GcsFilePublisher(
    private val config: GcsConfig,
    private val storage: Storage,
    private val storagePath: Path,
) : FilePublisher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(
        stopPlacePath: Path,
        timetablePaths: Map<String, Path>,
    ): Path {
        val stopBlobName = storagePath.resolve(FilePublisher.STOPS_DIR).resolve(stopPlacePath.fileName)
        val stopBlobInfo = BlobInfo.newBuilder(BlobId.of(config.outputBucketName, stopBlobName.joinToString("/"))).build()

        logger.info("Uploading stop place file: ${stopPlacePath.fileName}")
        Files.newInputStream(stopPlacePath).use { storage.createFrom(stopBlobInfo, it) }

        timetablePaths.forEach { (provider, timetablePath) ->
            MDC.putCloseable(PROVIDER, provider).use {
                logger.info("Uploading timetable zip for provider: {}", kv(PROVIDER, provider))
                val zipFile = Files.createTempFile(provider, ".zip")
                try {
                    zipDirectory(timetablePath, zipFile)
                    val timetableBlobName =
                        storagePath
                            .resolve(FilePublisher.TIMETABLE_DIR)
                            .resolve("$provider.zip")
                    val timetableBlobInfo =
                        BlobInfo
                            .newBuilder(
                                BlobId.of(config.outputBucketName, timetableBlobName.joinToString("/")),
                            ).build()
                    Files.newInputStream(zipFile).use { storage.createFrom(timetableBlobInfo, it) }
                } finally {
                    Files.deleteIfExists(zipFile)
                }
            }
        }

        logger.info("Successfully uploaded filtered files to Ubelluris bucket.")
        return Path.of(config.outputBucketName).resolve(stopBlobName)
    }
}

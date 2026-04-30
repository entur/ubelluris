package org.entur.ror.ubelluris.publish

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.publish.FilePublisher.Companion.STOPS_DIR
import org.entur.ror.ubelluris.publish.FilePublisher.Companion.TIMETABLE_DIR
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class GcsFilePublisher(
    private val config: GcsConfig,
    private val storage: Storage,
    private val storagePath: Path
) : FilePublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(stopPlacePath: Path, timetablePaths: Map<String, Path>): Path {
        val stopBlobName = storagePath.resolve(STOPS_DIR).resolve(stopPlacePath.fileName)
        val stopBlobInfo = BlobInfo.newBuilder(BlobId.of(config.outputBucketName, stopBlobName.joinToString("/"))).build()

        logger.info("Uploading stop place file to Ubelluris bucket")
        Files.newInputStream(stopPlacePath).use { storage.createFrom(stopBlobInfo, it) }

        timetablePaths.forEach { (provider, timetablePath) ->
            Files.walk(timetablePath).filter(Files::isRegularFile).forEach { file ->
                val timetableBlobName = storagePath.resolve(TIMETABLE_DIR).resolve(provider).resolve(file.fileName)
                val timetableBlobInfo = BlobInfo.newBuilder(BlobId.of(config.outputBucketName, timetableBlobName.joinToString("/"))).build()
                Files.newInputStream(file).use { storage.createFrom(timetableBlobInfo, it) }
            }
        }

        logger.info("Successfully uploaded filtered files to Ubelluris bucket.")
        return Path.of(config.outputBucketName).resolve(stopBlobName)
    }
}
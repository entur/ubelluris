package org.entur.ror.ubelluris.publish

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.entur.ror.ubelluris.config.GcsConfig
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class GcsFilePublisher(
    private val config: GcsConfig,
    private val storage: Storage,
    private val storagePath: Path
) : FilePublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(file: Path): Path {
        val blobName = storagePath.resolve(file.fileName)
        val blobId = BlobId.of(config.bucketName, blobName.joinToString("/"))
        val blobInfo = BlobInfo.newBuilder(blobId).build()

        logger.info("Uploading filtered file to Ubelluris bucket")
        Files.newInputStream(file).use { inputStream ->
            storage.createFrom(blobInfo, inputStream)
        }

        logger.info("Successfully uploaded filtered file to Ubelluris bucket.")
        return Path.of(config.bucketName).resolve(blobName)
    }
}
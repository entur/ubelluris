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
    private val storage: Storage
) : FilePublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(file: Path): Path {
        val blobName = file.fileName.toString()
        val blobId = BlobId.of(config.bucketName, blobName)
        val blobInfo = BlobInfo.newBuilder(blobId).build()

        logger.info("Uploading filtered file to Ubelluris bucket")
        Files.newInputStream(file).use { inputStream ->
            storage.createFrom(blobInfo, inputStream)
        }

        logger.info("Successfully uploaded filtered file to Ubelluris bucket.")
        return Path.of("gs://${config.bucketName}/${blobName}")
    }
}
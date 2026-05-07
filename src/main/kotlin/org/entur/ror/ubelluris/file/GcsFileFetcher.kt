package org.entur.ror.ubelluris.file

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.transfermanager.ParallelDownloadConfig
import com.google.cloud.storage.transfermanager.TransferManagerConfig
import com.google.cloud.storage.transfermanager.TransferStatus
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class GcsFileFetcher(
    private val storage: Storage,
    private val inputBucketName: String,
    private val stopPlaceBlobPath: String,
    private val timetableBlobPaths: Map<String, String>,
    private val downloadDir: Path = Path.of("downloads"),
) : FileFetcher {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val targetStopPlaceFile = "sweden_stop_places.xml"

    override fun fetch(): FileFetchResult {
        Files.createDirectories(downloadDir)
        downloadMissingBlobs()

        val stopOutputPath = downloadDir.resolve(stopPlaceBlobPath).resolveSibling(targetStopPlaceFile)
        val stopPlacePath =
            if (Files.exists(stopOutputPath)) {
                logger.info("Found existing download for $stopOutputPath")
                stopOutputPath
            } else {
                logger.info("Extracting XML from ${downloadDir.resolve(stopPlaceBlobPath)}")
                extractXmlFromZip(downloadDir.resolve(stopPlaceBlobPath), stopOutputPath)
            }

        val timetablePaths =
            timetableBlobPaths.mapValues { (provider, blobPath) ->
                val zipPath = downloadDir.resolve(blobPath)
                val extractDir = zipPath.resolveSibling(provider)
                if (Files.exists(extractDir)) {
                    logger.info("Using cached timetable dir: $extractDir")
                    extractDir
                } else {
                    Files.createDirectories(extractDir)
                    logger.info("Extracting timetable zip for $provider from $zipPath")
                    extractZipToDirectory(zipPath, extractDir)
                }
            }

        return FileFetchResult(stopPlacePath, timetablePaths)
    }

    private fun downloadMissingBlobs() {
        val allBlobPaths = listOf(stopPlaceBlobPath) + timetableBlobPaths.values
        val missingBlobs =
            allBlobPaths
                .filter { !Files.exists(downloadDir.resolve(it)) }
                .map { BlobInfo.newBuilder(BlobId.of(inputBucketName, it)).build() }

        if (missingBlobs.isEmpty()) {
            logger.info("All blobs already cached locally, skipping download")
            return
        }

        logger.info("Downloading ${missingBlobs.size} blob(s) in parallel from $inputBucketName")

        val downloadConfig =
            ParallelDownloadConfig
                .newBuilder()
                .setBucketName(inputBucketName)
                .setDownloadDirectory(downloadDir)
                .build()

        TransferManagerConfig
            .newBuilder()
            .setStorageOptions(storage.options)
            .build()
            .service
            .use { transferManager ->
                transferManager
                    .downloadBlobs(missingBlobs, downloadConfig)
                    .downloadResults
                    .filter { it.status != TransferStatus.SUCCESS }
                    .forEach { result ->
                        error("Failed to download ${result.input.name}, status: ${result.status} exception: ${result.exception}")
                    }
            }

        logger.info("Download complete")
    }
}

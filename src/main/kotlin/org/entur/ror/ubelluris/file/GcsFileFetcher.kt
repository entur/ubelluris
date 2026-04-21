package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Blob
import com.google.cloud.storage.Storage
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream

fun Storage.get(bucketName: String, blobPath: Path): Blob? = this.get(bucketName, blobPath.joinToString("/"))

class GcsFileFetcher(
    private val storage: Storage,
    private val inputBucketName: String,
    private val storagePath: Path,
    private val downloadDir: Path = Path.of("downloads")
) : FileFetcher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun fetch(): Path {
        val outputPath = downloadDir.resolve(storagePath).resolve("sweden_stop_places.xml")

        Files.createDirectories(downloadDir)

        if (Files.exists(outputPath)) {
            logger.info("Found existing download for $outputPath")
            return outputPath
        }

        val blobPath = storagePath.resolve("sweden.zip")
        logger.info("Fetching stops data from GCS: $inputBucketName/$blobPath")

        val blob = storage.get(inputBucketName, blobPath)
            ?: error("Blob not found: $inputBucketName/$blobPath")

        val zipBytes = blob.getContent()

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml")) {
                    Files.createDirectories(outputPath.parent)
                    Files.write(
                        outputPath,
                        zip.readBytes(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                    )
                    logger.info("Extracted and saved to: $outputPath")
                    return outputPath
                }
                entry = zip.nextEntry
            }
        }

        error("No XML file found in ZIP.")
    }
}

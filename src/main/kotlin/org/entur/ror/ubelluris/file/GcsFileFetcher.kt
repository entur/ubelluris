package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Storage
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import java.util.zip.ZipInputStream

class GcsFileFetcher(
    private val storage: Storage,
    private val inputBucketName: String,
    private val downloadDir: Path = Path.of("downloads")
) : FileFetcher {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun fetch(): Path {
        val today = LocalDate.now()
        val outputPath = downloadDir.resolve("${today}_stop_places.xml")

        Files.createDirectories(downloadDir)

        if (Files.exists(outputPath)) {
            logger.info("Found existing download for $today: $outputPath")
            return outputPath
        }

        val blobPath = "${today.year}/${"%02d".format(today.monthValue)}/${"%02d".format(today.dayOfMonth)}/sweden.zip"
        logger.info("Fetching stops data from GCS: $inputBucketName/$blobPath")

        val blob = storage.get(inputBucketName, blobPath)
            ?: error("Blob not found: $inputBucketName/$blobPath")

        val zipBytes = blob.getContent()

        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml")) {
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

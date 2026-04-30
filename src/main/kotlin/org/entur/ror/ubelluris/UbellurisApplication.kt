package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.config.JsonConfig
import org.entur.ror.ubelluris.file.GcsFileFetcher
import org.entur.ror.ubelluris.file.UbellurisBucketService
import org.entur.ror.ubelluris.filter.StopPlaceFilterService
import org.entur.ror.ubelluris.filter.TimetableNetexProcessor
import org.entur.ror.ubelluris.publish.LocalFilePublisher
import java.io.File
import java.time.LocalDate
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        printHelp()
        exitProcess(1)
    }

    val cliConfig = File(args[0])
        .inputStream()
        .use { inputStream ->
            JsonConfig.loadCliConfig(inputStream)
        }

    val blacklistFilePath = args.getOrElse(1) { "" }

    val gcsConfig = GcsConfig.fromEnvironment()
    val bucketService = UbellurisBucketService(gcsConfig)
    val storage = bucketService.createStorage()

    val today = LocalDate.now()
    val storagePath = Path("${today.year}", "%02d".format(today.monthValue), "%02d".format(today.dayOfMonth))

    UbellurisService(
        fetcher = GcsFileFetcher(
            storage = storage,
            inputBucketName = gcsConfig.inputBucketName,
            stopPlaceBlobPath = storagePath.resolve("stops/sweden.zip").joinToString("/"),
            timetableBlobPaths = cliConfig.timetableProviders.associateWith { provider ->
                storagePath.resolve("timetable/$provider.zip").joinToString("/")
            }
        ),
        timetableProcessor = TimetableNetexProcessor(cliConfig),
        stopPlaceProcessor = StopPlaceFilterService(
            cliConfig = cliConfig,
            blacklistFilePath = blacklistFilePath
        ),
        publisher = bucketService.createPublisher(storagePath, Path(cliConfig.resultsDir))
    ).run()
}

fun printHelp() {
    println(
        """
        Ubelluris takes one mandatory argument:
       - <cli-config-file-path>      : Path to the configuration file relative to the local directory (e.g. config/cli-config.json)
       An optional argument:
       - <blacklist-quays-file-path> : Path to the blacklist quays file (e.g. processing/blacklist-quays.txt)
    """.trimIndent()
    )
}

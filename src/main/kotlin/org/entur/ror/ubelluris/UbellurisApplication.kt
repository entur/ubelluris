package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.config.JsonConfig
import org.entur.ror.ubelluris.file.GcsFileFetcher
import org.entur.ror.ubelluris.file.UbellurisBucketService
import org.entur.ror.ubelluris.filter.FilterService
import org.entur.ror.ubelluris.timetable.TimetableProcessor
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.fetch.GcsTimetableFetcher
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
    val stopPlaceStoragePath = Path("${today.year}", "%02d".format(today.monthValue), "%02d".format(today.dayOfMonth), "stops")

    val timetableConfig = TimetableConfig(
        providers = cliConfig.timetableProviders,
        modeFilter = cliConfig.transportModes.toSet(),
        blacklist = emptyMap()
    )

    val timetableFetcher = GcsTimetableFetcher(timetableConfig, storage, gcsConfig.inputBucketName)
    val timetableProcessor = TimetableProcessor(timetableFetcher, timetableConfig)

    UbellurisService(
        fetcher = GcsFileFetcher(storage, gcsConfig.inputBucketName, stopPlaceStoragePath),
        processor = FilterService(
            cliConfig = cliConfig,
            timetableProcessor = timetableProcessor,
            blacklistFilePath = blacklistFilePath
        ),
        publisher = bucketService.createPublisher(stopPlaceStoragePath)
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

package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.config.ApiKeys
import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.config.JsonConfig
import org.entur.ror.ubelluris.file.HttpFileFetcher
import org.entur.ror.ubelluris.file.UbellurisBucketService
import org.entur.ror.ubelluris.filter.FilterService
import org.entur.ror.ubelluris.timetable.TimetableProcessor
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.fetch.HttpTimetableFetcher
import java.io.File
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

    val apiKeys = ApiKeys.fromEnvironment()
    val gcsConfig = GcsConfig.fromEnvironment()
    val bucketService = UbellurisBucketService(gcsConfig)

    val timetableConfig = TimetableConfig(
        apiUrl = cliConfig.timetableDataUrl,
        apiKey = apiKeys.timetableDataApiKey,
        providers = cliConfig.timetableProviders,
        modeFilter = cliConfig.transportModes.toSet(),
        blacklist = emptyMap()
    )

    val timetableFetcher = HttpTimetableFetcher(timetableConfig)
    val timetableProcessor = TimetableProcessor(timetableFetcher, timetableConfig)

    UbellurisService(
        fetcher = HttpFileFetcher("${cliConfig.stopsDataUrl}${apiKeys.stopsDataApiKey}"),
        processor = FilterService(
            cliConfig = cliConfig,
            timetableProcessor = timetableProcessor,
            blacklistFilePath = blacklistFilePath
        ),
        publisher = bucketService.createPublisher()
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


package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.config.ApiKeys
import org.entur.ror.ubelluris.config.JsonConfig
import org.entur.ror.ubelluris.file.HttpFileFetcher
import org.entur.ror.ubelluris.filter.FilterService
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.publish.LocalFilePublisher
import org.entur.ror.ubelluris.timetable.TimetableProcessor
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.fetch.HttpTimetableFetcher
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    if (args.size != 2) {
        printHelp()
        exitProcess(1)
    }

    val cliConfig = File(args[0])
        .inputStream()
        .use { inputStream ->
            JsonConfig.loadCliConfig(inputStream)
        }

    val apiKeys = ApiKeys.fromEnvironment()

    val blacklistFilePath = args[1]

    val timetableConfig = TimetableConfig(
        apiUrl = cliConfig.timetableDataUrl,
        apiKey = apiKeys.timetableDataApiKey,
        providers = cliConfig.timetableProviders,
        modeFilter = setOf(
            TransportMode.TRAM,
            TransportMode.WATER
        ),
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
        publisher = LocalFilePublisher()
    ).run()
}

fun printHelp() {
    println(
        """
        The app takes 2 arguments:
       - <cli-config-file-name>      : The name of the configuration file for CLI, relative to the local directory.
       - <blacklist-quays-file-path> : Path to the blacklist quays file (e.g., processing/blacklist-quays.txt)
    """.trimIndent()
    )
}


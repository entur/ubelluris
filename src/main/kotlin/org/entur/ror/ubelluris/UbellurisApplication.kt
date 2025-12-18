package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.config.JsonConfig
import org.entur.ror.ubelluris.file.HttpFileFetcher
import org.entur.ror.ubelluris.filter.FilterService
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.publish.LocalFilePublisher
import org.entur.ror.ubelluris.timetable.TimetableProcessor
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.fetch.HttpTimetableFetcher
import java.io.File

fun main(args: Array<String>) {

    if (args.size != 1) {
        printHelp()
        return
    }

    val cliConfig = File(args[0])
        .inputStream()
        .use { inputStream ->
            JsonConfig.loadCliConfig(inputStream)
        }

    val timetableConfig = TimetableConfig(
        apiUrl = cliConfig.timetableDataUrl,
        apiKey = cliConfig.timetableDataApiKey,
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
        fetcher = HttpFileFetcher("${cliConfig.stopsDataUrl}${cliConfig.stopsDataApiKey}"),
        processor = FilterService(timetableProcessor = timetableProcessor),
        publisher = LocalFilePublisher()
    ).run()
}

fun printHelp() {
    println(
        """
        The app takes 1 argument: 
       - <cli-config-file-name>      : The name of the configuration file for CLI, relative to the local directory.
    """.trimIndent()
    )
}


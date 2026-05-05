package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.config.JsonConfig
import org.entur.ror.ubelluris.file.FilePublisher.Companion.STOPS_DIR
import org.entur.ror.ubelluris.file.FilePublisher.Companion.TIMETABLE_DIR
import org.entur.ror.ubelluris.file.FileService
import org.entur.ror.ubelluris.filter.StopPlaceFilterService
import org.entur.ror.ubelluris.filter.TimetableFilterService
import java.io.File
import java.time.LocalDate
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printHelp()
        exitProcess(1)
    }

    val cliConfig =
        File(args[0])
            .inputStream()
            .use { inputStream ->
                JsonConfig.loadCliConfig(inputStream)
            }

    val blacklistFilePath = args.getOrElse(1) { "" }

    val gcsConfig = GcsConfig.fromEnvironment()
    val fileService = FileService(gcsConfig)

    val today = LocalDate.now()
    val storagePath = Path("${today.year}", "%02d".format(today.monthValue), "%02d".format(today.dayOfMonth))
    val stopPlaceBlobPath = storagePath.resolve("$STOPS_DIR/sweden.zip").joinToString("/")
    val timetableBlobPaths =
        cliConfig.timetableProviders.associateWith { provider ->
            storagePath.resolve("$TIMETABLE_DIR/$provider.zip").joinToString("/")
        }

    UbellurisService(
        fetcher = fileService.createFetcher(stopPlaceBlobPath, timetableBlobPaths),
        timetableProcessor = TimetableFilterService(cliConfig),
        stopPlaceProcessor =
            StopPlaceFilterService(
                cliConfig = cliConfig,
                blacklistFilePath = blacklistFilePath,
            ),
        publisher = fileService.createPublisher(storagePath, Path(cliConfig.resultsDir)),
    ).run()
}

fun printHelp() {
    println(
        """
         Ubelluris takes one mandatory argument:
        - <cli-config-file-path>      : Path to the configuration file relative to the local directory (e.g. config/cli-config.json)
        An optional argument:
        - <blacklist-quays-file-path> : Path to the blacklist quays file (e.g. processing/blacklist-quays.txt)
        """.trimIndent(),
    )
}

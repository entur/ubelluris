package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.config.JsonConfig
import org.entur.ror.ubelluris.file.FileService
import org.entur.ror.ubelluris.filter.StopPlaceFilterService
import org.entur.ror.ubelluris.filter.TimetableFilterService
import org.entur.ror.ubelluris.sales.SalesDataService
import java.io.File
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size < 4) {
        printHelp()
        exitProcess(1)
    }

    val cliConfig =
        File(args[0])
            .inputStream()
            .use { inputStream ->
                JsonConfig.loadCliConfig(inputStream)
            }

    val stopPlaceBlobPath = args[1]
    val timetableInputPath = Path(args[2])
    val salesInputPath = args[3]
    val outputPath = Path(args[4])
    val blacklistFilePath = args.getOrElse(5) { "" }

    val gcsConfig = GcsConfig.fromEnvironment()
    val fileService = FileService(gcsConfig)

    val timetableBlobPaths =
        cliConfig.timetableProviders.associateWith { provider ->
            timetableInputPath.resolve("$provider.zip").joinToString("/")
        }

    UbellurisService(
        fetcher = fileService.createFetcher(stopPlaceBlobPath, timetableBlobPaths, salesInputPath),
        timetableProcessor = TimetableFilterService(cliConfig),
        stopPlaceProcessor =
            StopPlaceFilterService(
                cliConfig = cliConfig,
                blacklistFilePath = blacklistFilePath,
            ),
        salesDataProcessor = SalesDataService(cliConfig),
        publisher = fileService.createPublisher(outputPath, Path(cliConfig.resultsDir)),
    ).run()
}

fun printHelp() {
    println(
        """
         Ubelluris takes four mandatory arguments:
        - <cli-config-file-path>      : Path to the configuration file relative to the local directory (e.g. config/cli-config.json)
        - <stop-place-zip-path>       : Path to the stop place zip or xml file (e.g. 2026/01/01/stops/sweden.zip)
        - <timetable-input-path>      : Path containing a <provider>.zip per timetable provider (e.g. 2026/01/01/timetable).
                                        Locally, a <provider> directory of XML files may be used instead of <provider>.zip
        - <sales-input-path>          : Path to the sales data file(s) (a zip file or a directory with xml files) (e.g. 2026/01/01/sales)
        - <output-path>               : Output path where stops/ and timetable/ results are written (e.g. 2026/01/01)
        An optional argument:
        - <blacklist-quays-file-path> : Path to the blacklist quays file (e.g. processing/blacklist-quays.txt)
        """.trimIndent(),
    )
}

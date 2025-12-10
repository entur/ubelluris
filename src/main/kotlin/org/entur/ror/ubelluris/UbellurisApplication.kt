package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.file.HttpFileFetcher
import org.entur.ror.ubelluris.filter.FilterService
import org.entur.ror.ubelluris.model.TransportMode
import org.entur.ror.ubelluris.publish.LocalFilePublisher
import org.entur.ror.ubelluris.timetable.TimetableBridgeProcessor
import org.entur.ror.ubelluris.timetable.config.TimetableConfig
import org.entur.ror.ubelluris.timetable.fetch.HttpTimetableFetcher

fun main() {
    val stopsApiKey = "foo"
    val stopsUrl = "https://opendata.samtrafiken.se/stopsregister-netex-sweden/sweden.zip?key=$stopsApiKey"

    val timetableConfig = TimetableConfig(
        apiUrl = "https://opendata.samtrafiken.se/netex/",
        apiKey = "FOO",
        providers = listOf("vt", "halland", "skane"),
        modeFilter = setOf(
            TransportMode.TRAM,
            TransportMode.WATER
        ),
        blacklist = emptyMap()
    )

    val timetableFetcher = HttpTimetableFetcher(timetableConfig)
    val timetableBridgeProcessor = TimetableBridgeProcessor(timetableFetcher, timetableConfig)

    UbellurisService(
        fetcher = HttpFileFetcher(stopsUrl),
        processor = FilterService(timetableBridgeProcessor = timetableBridgeProcessor),
        publisher = LocalFilePublisher()
    ).run()
}


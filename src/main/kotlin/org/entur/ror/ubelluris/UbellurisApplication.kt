package org.entur.ror.ubelluris

import org.entur.ror.ubelluris.file.HttpFileFetcher
import org.entur.ror.ubelluris.filter.FilterService
import org.entur.ror.ubelluris.publish.LocalFilePublisher

fun main() {
    val apiKey = "foo"
    val url = "https://opendata.samtrafiken.se/stopsregister-netex-sweden/sweden.zip?key=$apiKey"

    UbellurisService(
        fetcher = HttpFileFetcher(url),
        processor = FilterService(),
        publisher = LocalFilePublisher()
    ).run()
}

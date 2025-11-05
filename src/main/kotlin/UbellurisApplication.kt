import file.HttpFileFetcher
import filter.FilterService
import publish.LocalFilePublisher

fun main() {
    val apiKey = "foo"
    val url = "https://opendata.samtrafiken.se/stopsregister-netex-sweden/sweden.zip?key=$apiKey"

    UbellurisService(
        fetcher = HttpFileFetcher(url),
        processor = FilterService(),
        publisher = LocalFilePublisher()
    ).run()
}

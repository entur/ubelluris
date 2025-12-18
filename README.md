# Ubelluris

Ubelluris performs a transformation and filtering job on NeTEx PublicationDelivery datasets.

> Ubelluris is still a work in progress!
> It is currently tailored to filter and transform data from Trafiklab only!

## How it works
Ubelluris downloads stop place and timetable data from Trafiklab, and subsequently processes it to produce a filtered .xml file.

## Running Ubelluris locally
A minimal local setup requires Trafiklab API keys for "**Stops data**" and "**NeTEx Regional Static data**".

The main function takes one argument:
* A CLI config file

Running the main method in UbellurisApplication.kt with the necessary API keys will download stops data and produce a filtered result file.

### cli-config file

```json
{
  "stopsDataUrl": "https://urlForStopsData",
  "stopsDataApiKey": "stopsDataApiKey",
  "timetableDataUrl": "https://urlForTimetableData",
  "timetableDataApiKey": "timetableDataApiKey",
  "timetableProviders": [
    "provider1",
    "provider2",
    "provider3"
  ]
}
```
# Ubelluris

Ubelluris performs a transformation and filtering job on NeTEx PublicationDelivery datasets.

> Ubelluris is still a work in progress!

## How it works
Ubelluris downloads stop place and timetable data from Trafiklab, and subsequently processes it to produce a filtered .xml file.

## Running Ubelluris locally
A minimal local setup requires a Trafiklab API key for "Stops data".
Running the main method in UbellurisApplication.kt with a valid API key will download stops data and produce a result filtered file.
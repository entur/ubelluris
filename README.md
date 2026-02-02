# Ubelluris

Ubelluris performs a transformation and filtering job on NeTEx PublicationDelivery datasets.

> Ubelluris is still a work in progress!
> It is currently tailored to filter and transform data from Trafiklab only!

## How it works
Ubelluris downloads stop place and timetable data from Trafiklab, and subsequently processes it to produce a filtered .xml file.

## Running Ubelluris locally
A minimal local setup requires Trafiklab API keys for "**Stops data**" and "**NeTEx Regional Static data**".

Add ```STOPS_DATA_API_KEY=<your_stops_data_api_key>``` and ```TIMETABLE_DATA_API_KEY=<your_timetable_data_api_key>``` to environment variables.

The main function takes one argument:
* Path to a .json configuration file 

A second argument may be provided: 
* Path to a .txt file specifying any blacklisted quays

Running the main method in UbellurisApplication.kt with the necessary API keys will download stops data and produce a filtered result file.

### cli-config file

```json
{
  "stopsDataUrl": "https://urlForStopsData",
  "timetableDataUrl": "https://urlForTimetableData",
  "timetableProviders": [
    "provider1",
    "provider2",
    "provider3"
  ],
  "illegalPublicCodes": [
    "*",
    "-"
  ]
}
```

### blacklisted-quays.txt

```plain text
SE:050:Quay:0000
SE:050:Quay:0001
```

## Publishing to GCS Bucket

Ubelluris can publish results to a Google Cloud Storage bucket.

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GCS_UPLOAD_ENABLED` | No | Set to `true` to enable GCS publishing (default: `false`) |
| `GCS_PROJECT_ID` | When enabled | GCP project ID |
| `GCS_BUCKET_NAME` | When enabled | Target GCS bucket name |

### Behavior

- **Enabled**: Files are uploaded to the specified GCS bucket
- **Disabled** (default): Files are saved locally to a `results/` directory
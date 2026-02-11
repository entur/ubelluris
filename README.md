# Ubelluris

Ubelluris performs a transformation and filtering job on NeTEx PublicationDelivery datasets.

## How it works
Ubelluris fetches stop place and timetable data from a GCS (Google Cloud Storage) input bucket, and subsequently processes it to produce a filtered .xml file.

## Running Ubelluris locally
A minimal local setup requires access to a GCS input bucket containing stop place and timetable data.

The main function takes one argument:
* Path to a .json configuration file

A second argument may be provided:
* Path to a .txt file specifying any blacklisted quays

Running the main method in UbellurisApplication.kt with the necessary GCS configuration will fetch stops data and produce a filtered result file.

### cli-config file

```json
{
  "sourceCodespace": "replaceThis",
  "targetCodespace": "withThis",
  "timetableProviders": [
    "provider1",
    "provider2",
    "provider3"
  ],
  "transportModes": [
    "tram",
    "water"
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

## GCS Configuration

Ubelluris fetches input data from and can publish results to Google Cloud Storage buckets.

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `GCS_PROJECT_ID` | Yes | GCP project ID |
| `GCS_INPUT_BUCKET` | Yes | Source GCS bucket containing stop place and timetable data |
| `GCS_UPLOAD_ENABLED` | No | Set to `true` to enable GCS publishing (default: `false`) |
| `GCS_BUCKET_NAME` | When upload enabled | Target GCS bucket for publishing results |

### Behavior

- Input data (stops and timetables) is always fetched from the `GCS_INPUT_BUCKET`
- **Upload enabled**: Result files are uploaded to the specified `GCS_BUCKET_NAME`
- **Upload disabled** (default): Result files are saved locally to a `results/` directory
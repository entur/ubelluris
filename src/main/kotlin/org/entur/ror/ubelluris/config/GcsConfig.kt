package org.entur.ror.ubelluris.config

data class GcsConfig(
    val projectId: String,
    val outputBucketName: String,
    val inputBucketName: String,
    val uploadEnabled: Boolean,
    val downloadEnabled: Boolean,
) {
    companion object {
        fun fromEnvironment(): GcsConfig {
            val uploadEnabled = System.getenv("GCS_UPLOAD_ENABLED")?.toBoolean() ?: false
            val downloadEnabled = System.getenv("GCS_DOWNLOAD_ENABLED")?.toBoolean() ?: true

            if (!uploadEnabled && !downloadEnabled) {
                return GcsConfig(
                    projectId = "",
                    outputBucketName = "",
                    inputBucketName = "",
                    uploadEnabled = false,
                    downloadEnabled = false,
                )
            }

            val projectId =
                System.getenv("GCS_PROJECT_ID")
                    ?: throw IllegalStateException("GCS_PROJECT_ID environment variable not set")
            val outputBucketName =
                if (uploadEnabled) {
                    System.getenv("GCS_OUTPUT_BUCKET")
                        ?: throw IllegalStateException("GCS_OUTPUT_BUCKET environment variable not set")
                } else {
                    ""
                }
            val inputBucketName =
                if (downloadEnabled) {
                    System.getenv("GCS_INPUT_BUCKET")
                        ?: throw IllegalStateException("GCS_INPUT_BUCKET environment variable not set")
                } else {
                    ""
                }

            return GcsConfig(projectId, outputBucketName, inputBucketName, uploadEnabled, downloadEnabled)
        }
    }
}

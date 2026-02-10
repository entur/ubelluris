package org.entur.ror.ubelluris.config

data class GcsConfig(
    val projectId: String,
    val bucketName: String,
    val inputBucketName: String,
    val gcsEnabled: Boolean
) {
    companion object {
        fun fromEnvironment(): GcsConfig {
            val enabled = System.getenv("GCS_UPLOAD_ENABLED")?.toBoolean() ?: false

            if (!enabled) {
                return GcsConfig(
                    projectId = "",
                    bucketName = "",
                    inputBucketName = "",
                    gcsEnabled = false
                )
            }

            val projectId = System.getenv("GCS_PROJECT_ID")
                ?: throw IllegalStateException("GCS_PROJECT_ID environment variable not set")
            val bucketName = System.getenv("GCS_BUCKET_NAME")
                ?: throw IllegalStateException("GCS_BUCKET_NAME environment variable not set")
            val inputBucketName = System.getenv("GCS_INPUT_BUCKET")
                ?: throw IllegalStateException("GCS_INPUT_BUCKET environment variable not set")

            return GcsConfig(projectId, bucketName, inputBucketName, enabled)
        }
    }
}

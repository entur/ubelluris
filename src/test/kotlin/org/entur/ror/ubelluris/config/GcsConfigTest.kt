package org.entur.ror.ubelluris.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GcsConfigTest {

    @Test
    fun shouldCreateConfigWithValuesFromConstructor() {
        val config = GcsConfig(
            projectId = "test-project",
            outputBucketName = "test-bucket",
            inputBucketName = "test-input-bucket",
            gcsEnabled = true
        )

        assertThat(config.gcsEnabled).isEqualTo(true)
        assertThat(config.projectId).isEqualTo("test-project")
        assertThat(config.outputBucketName).isEqualTo("test-bucket")
        assertThat(config.inputBucketName).isEqualTo("test-input-bucket")
    }
}

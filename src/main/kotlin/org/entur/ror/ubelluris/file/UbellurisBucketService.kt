package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.publish.CompositeFilePublisher
import org.entur.ror.ubelluris.publish.FilePublisher
import org.entur.ror.ubelluris.publish.GcsFilePublisher
import org.entur.ror.ubelluris.publish.LocalFilePublisher

class UbellurisBucketService(
    private val config: GcsConfig,
    private val storageProvider: () -> Storage = {
        StorageOptions.newBuilder()
            .setProjectId(config.projectId)
            .build()
            .service
    }
) {
    fun createPublisher(): FilePublisher {
        if (!config.enabled) {
            return LocalFilePublisher()
        }

        val storage = storageProvider()
        return CompositeFilePublisher(
            listOf(
                GcsFilePublisher(config, storage),
                LocalFilePublisher()
            )
        )
    }
}

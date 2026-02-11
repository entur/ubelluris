package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.entur.ror.ubelluris.config.GcsConfig
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
    fun createStorage(): Storage {
        return storageProvider()
    }

    fun createPublisher(): FilePublisher {
        if (config.gcsEnabled) {
            val storage = createStorage()
            return GcsFilePublisher(config, storage)
        }

        return LocalFilePublisher()
    }
}

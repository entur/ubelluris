package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.entur.ror.ubelluris.config.GcsConfig
import org.entur.ror.ubelluris.publish.FilePublisher
import org.entur.ror.ubelluris.publish.GcsFilePublisher
import org.entur.ror.ubelluris.publish.LocalFilePublisher
import java.nio.file.Path

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

    fun createPublisher(storagePath: Path): FilePublisher {
        if (config.gcsEnabled) {
            val storage = createStorage()
            return GcsFilePublisher(config, storage, storagePath)
        }

        return LocalFilePublisher(storagePath)
    }
}

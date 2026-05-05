package org.entur.ror.ubelluris.file

import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import org.entur.ror.ubelluris.config.GcsConfig
import java.nio.file.Path

class FileService(
    private val config: GcsConfig,
    private val storageProvider: () -> Storage = {
        StorageOptions
            .newBuilder()
            .setProjectId(config.projectId)
            .build()
            .service
    },
) {
    fun createStorage(): Storage = storageProvider()

    fun createFetcher(
        stopPlaceBlobPath: String,
        timetableBlobPaths: Map<String, String>,
    ): FileFetcher {
        if (config.downloadEnabled) {
            val storage = createStorage()
            return GcsFileFetcher(
                storage = storage,
                config = config,
                stopPlaceBlobPath = stopPlaceBlobPath,
                timetableBlobPaths = timetableBlobPaths,
            )
        }

        return LocalFileFetcher(stopPlaceBlobPath, timetableBlobPaths)
    }

    fun createPublisher(
        storagePath: Path,
        localCachePath: Path,
    ): FilePublisher {
        if (config.uploadEnabled) {
            val storage = createStorage()
            return GcsFilePublisher(config, storage, storagePath)
        }

        return LocalFilePublisher(storagePath, localCachePath)
    }
}

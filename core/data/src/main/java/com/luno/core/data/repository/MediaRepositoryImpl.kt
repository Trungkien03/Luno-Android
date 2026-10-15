package com.luno.core.data.repository

import com.luno.core.data.datasource.MediaRemoteDataSource
import com.luno.core.domain.model.MediaType
import com.luno.core.domain.repository.MediaRepository

class MediaRepositoryImpl(
    private val remoteDataSource: MediaRemoteDataSource
) : MediaRepository {
    override suspend fun uploadMedia(
        bytes: ByteArray,
        fileName: String,
        mediaType: MediaType
    ): Result<String> {
        return try {
            val bucketName = when (mediaType) {
                MediaType.IMAGE -> "chat-images"
                MediaType.AUDIO -> "chat-audio"
                MediaType.FILE -> "chat-files"
                MediaType.TEXT -> "chat-media"
            }
            val url = remoteDataSource.uploadFile(bytes, fileName, bucketName)
            Result.success(url)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

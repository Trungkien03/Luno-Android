package com.luno.core.domain.repository

import com.luno.core.domain.model.MediaType

interface MediaRepository {
    suspend fun uploadMedia(
        bytes: ByteArray,
        fileName: String,
        mediaType: MediaType
    ): Result<String>
}

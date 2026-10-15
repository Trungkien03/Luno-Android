package com.luno.core.data.datasource

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.util.UUID

class MediaRemoteDataSource(
    private val supabaseClient: SupabaseClient
) {
    suspend fun uploadFile(
        bytes: ByteArray,
        fileName: String,
        bucketName: String = "chat-media"
    ): String {
        val extension = fileName.substringAfterLast('.', "")
        val uniqueName =
            "${UUID.randomUUID()}_${System.currentTimeMillis()}.${extension.ifEmpty { "bin" }}"
        val bucket = supabaseClient.storage[bucketName]

        bucket.upload(path = uniqueName, data = bytes) {
            upsert = true
        }

        return bucket.publicUrl(uniqueName)
    }

    suspend fun deleteFile(
        publicUrl: String,
        bucketName: String = "chat-images"
    ) {
        try {
            val fileName = publicUrl.substringAfterLast("/")
            if (fileName.isNotBlank()) {
                supabaseClient.storage[bucketName].delete(fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

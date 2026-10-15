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
}

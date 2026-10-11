package com.example.applenotes

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Disk cache of note bodies used for full-text search.
 *
 * The in-memory index is rebuilt on every process start; without this store
 * that means re-downloading every note body whenever Android (or an OEM
 * task killer) reclaims the process — even if the user only switched away
 * for a moment. Entries are keyed by recordName and invalidated when
 * [CachedBody.modMs] no longer matches the note's ModificationDate.
 */
object SearchBodyIndexStore {
    private const val TAG = "SearchBodyIndex"
    private const val FILE_NAME = "search_body_index.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val mutex = Mutex()

    @Serializable
    data class CachedBody(
        val modMs: Long,
        val text: String,
    )

    @Serializable
    private data class CacheFile(
        val entries: Map<String, CachedBody> = emptyMap(),
    )

    suspend fun load(context: Context): Map<String, CachedBody> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = file(context)
            if (!file.exists()) return@withLock emptyMap()
            runCatching {
                json.decodeFromString(CacheFile.serializer(), file.readText()).entries
            }.getOrElse { e ->
                Log.w(TAG, "failed to read cache: ${e.message}")
                emptyMap()
            }
        }
    }

    suspend fun save(context: Context, entries: Map<String, CachedBody>) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val file = file(context)
                runCatching {
                    file.writeText(json.encodeToString(CacheFile.serializer(), CacheFile(entries)))
                }.onFailure { e ->
                    Log.w(TAG, "failed to write cache: ${e.message}")
                }
            }
        }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching { file(context).delete() }
        }
    }

    private fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)
}

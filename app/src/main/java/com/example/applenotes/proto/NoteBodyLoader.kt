package com.example.applenotes.proto

import android.util.Log
import com.example.applenotes.client.AppleNotesClient
import com.example.applenotes.client.NoteRecord
import com.example.applenotes.client.extractAssetDownloadUrl
import com.example.applenotes.client.extractAssetSize
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TAG = "AppleNotesBodyLoader"

/**
 * Where a note's body bytes came from. Inline [TextDataEncrypted] is what this
 * app can (attempt to) write; [TextDataAsset] is used for large / modern notes
 * and is read-only until we reverse the asset write path.
 */
enum class NoteBodySource {
    TEXT_DATA_ENCRYPTED,
    TEXT_DATA_ASSET,
    NONE,
}

/**
 * Resolved note body for the detail screen.
 *
 * [visibleText] is what Mac/iCloud would show (title is the first line when
 * present). [readOnly] is true when we must not offer edits — typically because
 * the body lived in a CKAsset, or we could not decode a writable shape.
 */
data class ResolvedNoteBody(
    val visibleText: String,
    val attrSpans: List<NoteBodyEditor.AttrSpan>,
    val source: NoteBodySource,
    val readOnly: Boolean,
    val protoKind: NoteProtoKind?,
    val titleFallback: String?,
    val statusMessage: String? = null,
) {
    val displayTitle: String
        get() {
            val fromBody = visibleText.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
            return fromBody
                ?: titleFallback?.takeIf { it.isNotBlank() }
                ?: "Untitled"
        }
}

/**
 * Load a note's visible body, preferring inline `TextDataEncrypted` and falling
 * back to downloading `TextDataAsset` (large-note CloudKit packing).
 */
object NoteBodyLoader {

    suspend fun resolve(client: AppleNotesClient, record: NoteRecord): ResolvedNoteBody {
        val titleFallback = record.decodedTitle()
        if (record.isPasswordLocked()) {
            Log.i(TAG, "resolve ${record.recordName}: password-locked — body not readable here")
            return ResolvedNoteBody(
                visibleText = "",
                attrSpans = emptyList(),
                source = NoteBodySource.NONE,
                readOnly = true,
                protoKind = null,
                titleFallback = titleFallback,
                statusMessage = "This note is locked with a password on Apple devices and can’t be opened here.",
            )
        }
        val inlineB64 = record.stringField("TextDataEncrypted")
        if (inlineB64 != null) {
            return decodeFromBase64(
                b64 = inlineB64,
                source = NoteBodySource.TEXT_DATA_ENCRYPTED,
                titleFallback = titleFallback,
                // Writable only when we recognize the legacy topotext shape.
                preferWritable = true,
            )
        }

        val assetField = record.rawFields["TextDataAsset"]
        val downloadUrl = assetField?.let { extractAssetDownloadUrl(it) }
        val assetSize = assetField?.let { extractAssetSize(it) }
        if (downloadUrl == null) {
            Log.w(TAG, "resolve ${record.recordName}: no TextDataEncrypted and no TextDataAsset URL")
            return ResolvedNoteBody(
                visibleText = "",
                attrSpans = emptyList(),
                source = NoteBodySource.NONE,
                readOnly = true,
                protoKind = null,
                titleFallback = titleFallback,
                statusMessage = "This note has no readable body on the server.",
            )
        }

        Log.i(
            TAG,
            "resolve ${record.recordName}: fetching TextDataAsset size=$assetSize url.len=${downloadUrl.length}",
        )
        val bytes = client.fetchAssetBytes(downloadUrl)
        if (bytes == null) {
            Log.e(TAG, "resolve ${record.recordName}: TextDataAsset download failed")
            return ResolvedNoteBody(
                visibleText = "",
                attrSpans = emptyList(),
                source = NoteBodySource.TEXT_DATA_ASSET,
                readOnly = true,
                protoKind = null,
                titleFallback = titleFallback,
                statusMessage = "Could not download this note’s body.",
            )
        }

        Log.i(
            TAG,
            "resolve ${record.recordName}: asset bytes=${bytes.size} magic=${bytes.copyOf(minOf(4, bytes.size)).toHex()}",
        )
        return decodeFromRawBytes(
            bytes = bytes,
            source = NoteBodySource.TEXT_DATA_ASSET,
            titleFallback = titleFallback,
            // Asset-backed notes are always read-only — write path unexplored.
            forceReadOnly = true,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeFromBase64(
        b64: String,
        source: NoteBodySource,
        titleFallback: String?,
        preferWritable: Boolean,
    ): ResolvedNoteBody {
        val bytes = runCatching { Base64.decode(b64) }.getOrElse {
            Log.w(TAG, "base64 decode failed: ${it.message}")
            return emptyFailure(source, titleFallback, "Could not decode note body.")
        }
        return decodeFromRawBytes(bytes, source, titleFallback, forceReadOnly = !preferWritable)
    }

    /**
     * Asset / encrypted payloads may arrive as:
     *  - gzip/zlib-compressed proto (same as TextDataEncrypted wire value)
     *  - raw proto
     *  - base64 text wrapping either of the above (defensive)
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeFromRawBytes(
        bytes: ByteArray,
        source: NoteBodySource,
        titleFallback: String?,
        forceReadOnly: Boolean,
    ): ResolvedNoteBody {
        val candidates = buildList {
            add(bytes)
            // Sometimes CDNs hand back already-decompressed proto; sometimes
            // the asset is still compressed. Try both.
            runCatching { Gzip.decompress(bytes) }.getOrNull()?.let { add(it) }
            // Defensive: asset body is ASCII base64 of the usual payload.
            if (looksLikeAsciiBase64(bytes)) {
                val decoded = runCatching { Base64.decode(bytes.decodeToString().trim()) }.getOrNull()
                if (decoded != null) {
                    add(decoded)
                    runCatching { Gzip.decompress(decoded) }.getOrNull()?.let { add(it) }
                }
            }
        }.distinctBy { it.contentHashCode() to it.size }

        for (candidate in candidates) {
            val kind = runCatching { NoteBodyEditor.probe(candidate) }.getOrDefault(NoteProtoKind.UNKNOWN)
            val visible = runCatching { NoteBodyEditor.readVisibleText(candidate) }.getOrNull()
                ?: runCatching { NoteBodyEditor.readText(candidate) }.getOrNull()
            if (visible != null) {
                val spans = runCatching { NoteBodyEditor.parseAttributeRuns(candidate) }.getOrElse { emptyList() }
                Log.i(
                    TAG,
                    "decoded source=$source kind=$kind chars=${visible.length} " +
                        "spans=${spans.size} (app is read-only)",
                )
                return ResolvedNoteBody(
                    visibleText = visible,
                    attrSpans = spans,
                    source = source,
                    readOnly = true,
                    protoKind = kind,
                    titleFallback = titleFallback,
                    statusMessage = null,
                )
            }
            Log.i(TAG, "candidate len=${candidate.size} kind=$kind did not yield topotext")
        }

        // Large / modern notes put MergeableData in TextDataAsset. Extract the
        // longest human-readable UTF-8 string as a read-only body.
        for (candidate in candidates) {
            val plain = MergeableDataDecoder.decodeNotePlainText(candidate) ?: continue
            Log.i(TAG, "decoded MergeableData plain text chars=${plain.length} source=$source")
            return ResolvedNoteBody(
                visibleText = plain,
                attrSpans = emptyList(),
                source = source,
                readOnly = true,
                protoKind = NoteProtoKind.MERGABLE_DATA,
                titleFallback = titleFallback,
                statusMessage = null,
            )
        }

        return emptyFailure(
            source = source,
            titleFallback = titleFallback,
            statusMessage = "Couldn’t decode this note’s body yet.",
        )
    }

    private fun emptyFailure(
        source: NoteBodySource,
        titleFallback: String?,
        statusMessage: String,
    ) = ResolvedNoteBody(
        visibleText = "",
        attrSpans = emptyList(),
        source = source,
        readOnly = true,
        protoKind = null,
        titleFallback = titleFallback,
        statusMessage = statusMessage,
    )

    private fun looksLikeAsciiBase64(bytes: ByteArray): Boolean {
        if (bytes.isEmpty() || bytes.size > 8_000_000) return false
        // Cheap heuristic: mostly base64 alphabet + whitespace, long enough.
        if (bytes.size < 32) return false
        var ok = 0
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            val good = c in 0x41..0x5A || c in 0x61..0x7A || c in 0x30..0x39 ||
                c == 0x2B || c == 0x2F || c == 0x3D || c == 0x0A || c == 0x0D || c == 0x20
            if (good) ok++
        }
        return ok * 100 / bytes.size >= 95
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }
}

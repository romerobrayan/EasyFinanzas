package dev.romerobrayan.tinto.core.data.capture

import dev.romerobrayan.tinto.core.domain.model.CaptureChannel

/**
 * A device channel that feeds bank messages into the [CapturePipeline]. SMS
 * is the only live source this sprint; the notification listener (Nu) and
 * Gmail implement this same seam later so the parser and staging store never
 * care which source fed them.
 */
interface CaptureSource {

    val channel: CaptureChannel

    /**
     * One-time bounded scan of the channel's history (e.g. the SMS inbox).
     * Idempotent — already-seen captures never stage twice — and a silent
     * no-op when the channel has no readable history or no permission.
     */
    suspend fun backfill()
}

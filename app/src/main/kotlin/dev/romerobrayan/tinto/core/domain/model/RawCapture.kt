package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.Instant

/**
 * A bank message exactly as a capture source delivered it, normalized to the
 * parser's input shape. Pure data — the Android glue that produces it lives in
 * `core/data/capture`, and the raw [body] never leaves the device.
 */
data class RawCapture(
    /** SMS shortcode ("85540", "891134") or, in later sprints, a package name. */
    val sender: String,
    val body: String,
    /** When the device received it; the date fallback for bodies without one. */
    val receivedAt: Instant,
    val channel: CaptureChannel,
)

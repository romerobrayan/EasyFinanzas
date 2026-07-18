package dev.romerobrayan.tinto.core.domain.model

import kotlinx.datetime.Instant

/**
 * A bank message as delivered by a capture source, normalized to plain data
 * before parsing. Pure Kotlin — no Android types — so the whole capture
 * pipeline below the source glue is JVM-testable.
 */
data class RawCapture(
    /** "85540", "891134", or a package name once notifications land. */
    val sender: String,
    val body: String,
    /** Date fallback for messages whose body carries no absolute date. */
    val receivedAt: Instant,
    val channel: CaptureChannel,
)

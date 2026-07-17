package dev.romerobrayan.tinto.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Every capture the pipeline has ever staged, keyed by a hash of
 * (sender | body). Confirm/discard delete the pending row but the key stays,
 * so a backfill re-run or a re-delivered broadcast can never stage the same
 * message twice.
 */
@Entity(tableName = "capture_seen")
data class CaptureSeenEntity(
    @PrimaryKey val rawKey: String,
    val seenAt: Long,
)

package dev.romerobrayan.tinto.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Device-local staging database — capture pipeline only. The committed ledger
 * stays behind the session-routed `Synced*` repositories (Firestore or
 * in-memory); nothing in here is ever replicated off the device.
 *
 * exportSchema is off while the schema holds only short-lived staging rows;
 * turn it on (with a schema directory) the day this stores anything worth a
 * migration.
 */
@Database(
    entities = [PendingTransactionEntity::class, CaptureSeenEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TintoDatabase : RoomDatabase() {

    abstract fun pendingTransactionDao(): PendingTransactionDao
}

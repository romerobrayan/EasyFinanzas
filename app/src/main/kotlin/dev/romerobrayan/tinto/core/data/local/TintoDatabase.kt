package dev.romerobrayan.tinto.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Device-local database. Deliberately small: the committed ledger lives in
 * Cloud Firestore — Room exists only for the capture staging layer
 * (`pending_transactions`), which is pre-user-data and never synced.
 */
@Database(
    entities = [PendingTransactionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TintoDatabase : RoomDatabase() {

    abstract fun pendingTransactionDao(): PendingTransactionDao
}

package dev.romerobrayan.tinto.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Device-local database. Deliberately small: the committed ledger lives in
 * Cloud Firestore — Room exists only for the capture staging layer
 * (`pending_transactions`), which is pre-user-data and never synced.
 */
/*
 * Version 2: [PendingTransactionEntity] gained the Sprint 4 notification dedup
 * bucket and the Sprint 5 income fields while the version stayed at 1, so Room's
 * identity hash stopped matching any database written by an earlier build and
 * every update crashed on launch. Bumping the version routes those installs
 * through the migration path, where the destructive fallback in DatabaseModule
 * recreates the table.
 */
@Database(
    entities = [PendingTransactionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class TintoDatabase : RoomDatabase() {

    abstract fun pendingTransactionDao(): PendingTransactionDao
}

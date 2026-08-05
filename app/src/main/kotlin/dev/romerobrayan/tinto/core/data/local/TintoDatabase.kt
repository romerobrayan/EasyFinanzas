package dev.romerobrayan.tinto.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Device-local database. Two very different kinds of data live here:
 *
 * - `pending_transactions` — capture staging, re-derivable from the inbox.
 * - `transactions` / `cards` / `reminders` / `recurring_rules` — the ledger of
 *   a no-account ([dev.romerobrayan.tinto.core.domain.model.UserSession.Local])
 *   user. Signed-in users keep theirs in Cloud Firestore; these rows have no
 *   copy anywhere, which is why version 3 onwards migrates instead of dropping.
 */
/*
 * Version 2: [PendingTransactionEntity] gained the Sprint 4 notification dedup
 * bucket and the Sprint 5 income fields while the version stayed at 1, so Room's
 * identity hash stopped matching any database written by an earlier build and
 * every update crashed on launch. Bumping the version routes those installs
 * through the migration path, where the destructive fallback in DatabaseModule
 * recreates the table.
 *
 * Version 3: the device-local ledger tables above (MIGRATION_2_3).
 */
@Database(
    entities = [
        PendingTransactionEntity::class,
        TransactionEntity::class,
        CardEntity::class,
        ReminderEntity::class,
        RecurringRuleEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class TintoDatabase : RoomDatabase() {

    abstract fun pendingTransactionDao(): PendingTransactionDao

    abstract fun transactionDao(): TransactionDao

    abstract fun cardDao(): CardDao

    abstract fun reminderDao(): ReminderDao

    abstract fun recurringRuleDao(): RecurringRuleDao
}

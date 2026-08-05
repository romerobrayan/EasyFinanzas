package dev.romerobrayan.tinto.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 3 adds the device-local ledger (`transactions`, `cards`,
 * `reminders`, `recurring_rules`) for no-account users.
 *
 * Hand-written rather than destructive: from here on this database holds data
 * the user typed and can only get back from a JSON export, so dropping it on a
 * schema change is not an option. The staged `pending_transactions` rows are
 * left untouched — adding tables touches nothing that exists.
 *
 * The statements must match what Room generates for the entities exactly
 * (column order aside); `app/schemas/…/3.json` is the reference to diff
 * against when this file changes.
 */
internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transactions` (" +
                "`id` TEXT NOT NULL, `type` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`method` TEXT NOT NULL, `cardId` TEXT, `bank` TEXT, " +
                "`categoryId` TEXT NOT NULL, `merchant` TEXT, " +
                "`occurredAtEpochMs` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                "`createdAtEpochMs` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_occurredAtEpochMs` " +
                "ON `transactions` (`occurredAtEpochMs`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` " +
                "ON `transactions` (`categoryId`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cards` (" +
                "`id` TEXT NOT NULL, `bank` TEXT NOT NULL, `last4` TEXT NOT NULL, " +
                "`label` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminders` (" +
                "`id` TEXT NOT NULL, `title` TEXT NOT NULL, `amountCents` INTEGER, " +
                "`dueDate` TEXT NOT NULL, `dueTime` TEXT, `recurrence` TEXT NOT NULL, " +
                "`isPaid` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_rules` (" +
                "`id` TEXT NOT NULL, `type` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`method` TEXT NOT NULL, `cardId` TEXT, `bank` TEXT, " +
                "`categoryId` TEXT NOT NULL, `merchant` TEXT, `frequency` TEXT NOT NULL, " +
                "`anchorDate` TEXT NOT NULL, `nextOccurrence` TEXT NOT NULL, " +
                "`isActive` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, " +
                "`updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}

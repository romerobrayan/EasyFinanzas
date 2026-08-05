package dev.romerobrayan.tinto.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/*
 * Reads are Flows so the device-local repositories behave exactly like the
 * Firestore listeners they stand in for. Writes are upserts by primary key:
 * add and update collapse into one call, which also makes an import of a file
 * the device already has a no-op instead of a duplicate.
 */

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY occurredAtEpochMs DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Upsert
    suspend fun upsert(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CardDao {

    @Query("SELECT * FROM cards")
    fun observeAll(): Flow<List<CardEntity>>

    @Upsert
    suspend fun upsert(entity: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY dueDate")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Upsert
    suspend fun upsert(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RecurringRuleDao {

    @Query("SELECT * FROM recurring_rules")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Upsert
    suspend fun upsert(entity: RecurringRuleEntity)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun delete(id: String)
}

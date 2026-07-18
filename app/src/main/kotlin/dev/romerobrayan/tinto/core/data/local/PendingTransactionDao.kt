package dev.romerobrayan.tinto.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Query(
        "SELECT * FROM pending_transactions WHERE status = 'PENDING' " +
            "ORDER BY occurredAtEpochMs DESC",
    )
    fun observePending(): Flow<List<PendingTransactionEntity>>

    /** Ignores rows whose dedupKey was already staged — backfill re-runs are no-ops. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingTransactionEntity): Long

    @Query("UPDATE pending_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

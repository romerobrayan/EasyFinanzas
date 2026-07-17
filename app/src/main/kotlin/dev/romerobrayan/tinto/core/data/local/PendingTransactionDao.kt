package dev.romerobrayan.tinto.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Query("SELECT * FROM pending_transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<PendingTransactionEntity>>

    /**
     * Stages atomically: the seen-key insert is the dedupe gate — when the key
     * already exists the pending row is not written and false comes back.
     */
    @Transaction
    suspend fun stageIfUnseen(entity: PendingTransactionEntity, seen: CaptureSeenEntity): Boolean {
        if (insertSeen(seen) == ALREADY_SEEN) return false
        insertPending(entity)
        return true
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSeen(seen: CaptureSeenEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(entity: PendingTransactionEntity)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    companion object {
        /** Room returns -1 from an IGNOREd insert whose key already exists. */
        const val ALREADY_SEEN = -1L
    }
}

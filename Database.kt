package com.deafcall.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
//  DAO
// ─────────────────────────────────────────────
@Dao
interface CallRecordDao {

    @Query("SELECT * FROM call_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<CallRecord>>

    @Query("SELECT * FROM call_records WHERE id = :id")
    suspend fun getRecordById(id: String): CallRecord?

    @Query("SELECT * FROM call_records WHERE phoneNumber = :phone ORDER BY startTime DESC")
    fun getRecordsByPhone(phone: String): Flow<List<CallRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: CallRecord)

    @Delete
    suspend fun deleteRecord(record: CallRecord)

    @Query("DELETE FROM call_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM call_records")
    fun getTotalCount(): Flow<Int>
}

// ─────────────────────────────────────────────
//  Database
// ─────────────────────────────────────────────
@Database(
    entities = [CallRecord::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(CallRecordConverters::class)
abstract class DeafCallDatabase : RoomDatabase() {
    abstract fun callRecordDao(): CallRecordDao

    companion object {
        const val DATABASE_NAME = "deafcall_db"
    }
}

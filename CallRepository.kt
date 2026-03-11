package com.deafcall.utils

import com.deafcall.model.CallRecord
import com.deafcall.model.CallRecordDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val dao: CallRecordDao
) {
    fun getAllRecords(): Flow<List<CallRecord>> = dao.getAllRecords()

    fun getRecordsByPhone(phone: String): Flow<List<CallRecord>> =
        dao.getRecordsByPhone(phone)

    suspend fun saveRecord(record: CallRecord) = dao.insertRecord(record)

    suspend fun deleteRecord(record: CallRecord) = dao.deleteRecord(record)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
}

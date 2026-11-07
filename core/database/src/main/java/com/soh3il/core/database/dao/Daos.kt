package com.soh3il.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.soh3il.core.database.entity.OccasionEntity
import com.soh3il.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY nextTriggerEpochMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY nextTriggerEpochMillis ASC")
    suspend fun getAllActiveRemindersSync(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day")
    fun getRemindersForDate(year: Int, month: Int, day: Int): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}

@Dao
interface OccasionDao {

    @Query("SELECT * FROM occasions WHERE jalaliYear = :year AND jalaliMonth = :month ORDER BY jalaliDay ASC")
    fun getOccasionsForMonth(year: Int, month: Int): Flow<List<OccasionEntity>>

    @Query("SELECT * FROM occasions WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day")
    fun getOccasionsForDay(year: Int, month: Int, day: Int): Flow<List<OccasionEntity>>

    @Query("SELECT * FROM occasions WHERE jalaliDate = :date")
    suspend fun getOccasionsByDateSync(date: String): List<OccasionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccasions(occasions: List<OccasionEntity>)
}

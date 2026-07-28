package com.rtwifianalyzer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "device_history")
data class DeviceEntity(@PrimaryKey val mac: String, val ip: String, val name: String, val manufacturer: String, val firstSeen: Long, val lastSeen: Long, val online: Boolean)

@Dao interface DeviceDao {
    @Query("SELECT * FROM device_history ORDER BY lastSeen DESC") fun observeAll(): Flow<List<DeviceEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(device: DeviceEntity)
}

@Database(entities = [DeviceEntity::class], version = 1, exportSchema = false)
abstract class DeviceDatabase : RoomDatabase() { abstract fun deviceDao(): DeviceDao }

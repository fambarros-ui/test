package com.patricia.luminails.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.patricia.luminails.domain.model.AppointmentStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class Converters {
    @TypeConverter fun fromLocalDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun fromLocalTime(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
    @TypeConverter fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()
    @TypeConverter fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)
    @TypeConverter fun fromStatus(value: AppointmentStatus?): String? = value?.name
    @TypeConverter fun toStatus(value: String?): AppointmentStatus? = value?.let(AppointmentStatus::valueOf)
}

@Database(
    entities = [ClientEntity::class, ServiceEntity::class, AppointmentEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun serviceDao(): ServiceDao
    abstract fun appointmentDao(): AppointmentDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "luminails.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}

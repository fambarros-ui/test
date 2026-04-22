package com.patricia.luminails.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.patricia.luminails.domain.model.AppointmentStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultPrice: Double,
    val durationMinutes: Int? = null
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
    val clientId: Long,
    val serviceId: Long? = null,
    val serviceNameSnapshot: String = "",
    val servicePriceSnapshot: Double = 0.0,
    val notes: String = "",
    val status: AppointmentStatus = AppointmentStatus.MARCADA
)

package com.patricia.luminails.domain.model

import java.time.LocalDate
import java.time.LocalTime

enum class AppointmentStatus { MARCADA, CONCLUIDA, CANCELADA }

data class DashboardSummary(
    val todayCount: Int = 0,
    val nextClient: String? = null,
    val upcoming: List<AppointmentWithDetails> = emptyList(),
    val dayRevenue: Double = 0.0,
    val monthRevenue: Double = 0.0
)

data class AppointmentWithDetails(
    val id: Long,
    val date: LocalDate,
    val time: LocalTime,
    val clientId: Long,
    val clientName: String,
    val serviceId: Long?,
    val serviceName: String,
    val price: Double,
    val notes: String,
    val status: AppointmentStatus
)

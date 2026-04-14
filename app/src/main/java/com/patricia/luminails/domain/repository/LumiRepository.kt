package com.patricia.luminails.domain.repository

import com.patricia.luminails.data.local.AppointmentDao
import com.patricia.luminails.data.local.AppointmentEntity
import com.patricia.luminails.data.local.ClientDao
import com.patricia.luminails.data.local.ClientEntity
import com.patricia.luminails.data.local.ServiceDao
import com.patricia.luminails.data.local.ServiceEntity
import com.patricia.luminails.domain.model.AppointmentStatus
import com.patricia.luminails.domain.model.AppointmentWithDetails
import com.patricia.luminails.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

class LumiRepository(
    private val clientDao: ClientDao,
    private val serviceDao: ServiceDao,
    private val appointmentDao: AppointmentDao
) {
    fun observeClients(query: String): Flow<List<ClientEntity>> =
        if (query.isBlank()) clientDao.observeClients() else clientDao.searchClients(query)

    fun observeServices(): Flow<List<ServiceEntity>> = serviceDao.observeServices()

    fun observeAppointments(): Flow<List<AppointmentWithDetails>> = combine(
        appointmentDao.observeAppointments(),
        clientDao.observeClients()
    ) { appointments, clients ->
        val cMap = clients.associateBy { it.id }
        appointments.map {
            AppointmentWithDetails(
                id = it.id,
                date = it.date,
                time = it.time,
                clientId = it.clientId,
                clientName = cMap[it.clientId]?.name ?: "Cliente",
                serviceId = it.serviceId,
                serviceName = it.serviceNameSnapshot,
                price = it.servicePriceSnapshot,
                notes = it.notes,
                status = it.status
            )
        }
    }

    fun observeDashboard(today: LocalDate = LocalDate.now()): Flow<DashboardSummary> = observeAppointments().combine(observeServices()) { appts, _ ->
        val todayAppts = appts.filter { it.date == today }
        val next = todayAppts.firstOrNull { it.time >= LocalTime.now() && it.status == AppointmentStatus.MARCADA }
        DashboardSummary(
            todayCount = todayAppts.size,
            nextClient = next?.clientName,
            upcoming = appts.filter { it.date >= today }.take(6),
            dayRevenue = todayAppts.filter { it.status == AppointmentStatus.CONCLUIDA }.sumOf { it.price },
            monthRevenue = appts.filter { it.date.month == today.month && it.date.year == today.year && it.status == AppointmentStatus.CONCLUIDA }.sumOf { it.price }
        )
    }

    suspend fun saveClient(client: ClientEntity) {
        if (client.id == 0L) clientDao.insert(client) else clientDao.update(client)
    }

    suspend fun deleteClient(client: ClientEntity) = clientDao.delete(client)
    suspend fun getClient(id: Long) = clientDao.getById(id)

    suspend fun saveService(service: ServiceEntity) {
        if (service.id == 0L) serviceDao.insert(service) else serviceDao.update(service)
    }

    suspend fun deleteService(service: ServiceEntity) = serviceDao.delete(service)

    suspend fun saveAppointment(appointment: AppointmentEntity) {
        if (appointment.id == 0L) appointmentDao.insert(appointment) else appointmentDao.update(appointment)
    }

    suspend fun getAppointment(id: Long) = appointmentDao.getById(id)
    suspend fun deleteAppointment(appointment: AppointmentEntity) = appointmentDao.delete(appointment)

    suspend fun seedIfEmpty() {
        if (appointmentDao.count() > 0) return
        val c1 = clientDao.insert(ClientEntity(name = "Ana Martins", phone = "912345678", notes = "Prefere tons nude"))
        val c2 = clientDao.insert(ClientEntity(name = "Beatriz Costa", phone = "934567890", notes = "Alérgica a acetona forte"))
        val c3 = clientDao.insert(ClientEntity(name = "Carla Sousa", phone = "965432187", notes = "Manutenção mensal"))

        val s1 = serviceDao.insert(ServiceEntity(name = "Verniz Gel", defaultPrice = 22.0, durationMinutes = 60))
        val s2 = serviceDao.insert(ServiceEntity(name = "Manutenção Gel", defaultPrice = 28.0, durationMinutes = 90))
        val s3 = serviceDao.insert(ServiceEntity(name = "Nail Art", defaultPrice = 10.0, durationMinutes = 30))

        val today = LocalDate.now()
        listOf(
            AppointmentEntity(date = today, time = LocalTime.of(10, 0), clientId = c1, serviceId = s1, serviceNameSnapshot = "Verniz Gel", servicePriceSnapshot = 22.0, status = AppointmentStatus.MARCADA),
            AppointmentEntity(date = today, time = LocalTime.of(14, 30), clientId = c2, serviceId = s2, serviceNameSnapshot = "Manutenção Gel", servicePriceSnapshot = 28.0, status = AppointmentStatus.CONCLUIDA),
            AppointmentEntity(date = today.plusDays(1), time = LocalTime.of(11, 0), clientId = c3, serviceId = s3, serviceNameSnapshot = "Nail Art", servicePriceSnapshot = 10.0, notes = "Flores minimalistas"),
            AppointmentEntity(date = today.minusDays(2), time = LocalTime.of(15, 0), clientId = c1, serviceId = s2, serviceNameSnapshot = "Manutenção Gel", servicePriceSnapshot = 28.0, status = AppointmentStatus.CONCLUIDA)
        ).forEach { appointmentDao.insert(it) }
    }

    suspend fun appointmentsByClient(clientId: Long): List<AppointmentEntity> =
        appointmentDao.observeAppointments().first().filter { it.clientId == clientId }
}

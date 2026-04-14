package com.patricia.luminails.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.patricia.luminails.data.local.AppointmentEntity
import com.patricia.luminails.data.local.ClientEntity
import com.patricia.luminails.data.local.ServiceEntity
import com.patricia.luminails.domain.model.AppointmentStatus
import com.patricia.luminails.domain.model.AppointmentWithDetails
import com.patricia.luminails.domain.model.DashboardSummary
import com.patricia.luminails.domain.repository.LumiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MainViewModel(private val repo: LumiRepository) : ViewModel() {
    val clientQuery = MutableStateFlow("")
    val appointmentQuery = MutableStateFlow("")
    val appointmentStatusFilter = MutableStateFlow<AppointmentStatus?>(null)
    val selectedDate = MutableStateFlow(LocalDate.now())

    val clients: StateFlow<List<ClientEntity>> = clientQuery
        .flatMapLatest { repo.observeClients(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val services: StateFlow<List<ServiceEntity>> = repo.observeServices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppointments: StateFlow<List<AppointmentWithDetails>> = repo.observeAppointments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appointments: StateFlow<List<AppointmentWithDetails>> = combine(
        allAppointments, appointmentQuery, appointmentStatusFilter
    ) { appts, query, status ->
        appts.filter {
            (query.isBlank() || it.clientName.contains(query, true) || it.serviceName.contains(query, true)) &&
                (status == null || it.status == status)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyAppointments: StateFlow<List<AppointmentWithDetails>> = combine(allAppointments, selectedDate) { appts, date ->
        appts.filter { it.date == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboard: StateFlow<DashboardSummary> = repo.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    init {
        viewModelScope.launch { repo.seedIfEmpty() }
    }

    fun saveClient(id: Long = 0, name: String, phone: String, notes: String, onDone: () -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.saveClient(ClientEntity(id = id, name = name.trim(), phone = phone.trim(), notes = notes.trim()))
            onDone()
        }
    }

    fun deleteClient(client: ClientEntity) = viewModelScope.launch { repo.deleteClient(client) }

    fun saveService(id: Long = 0, name: String, price: Double, duration: Int?, onDone: () -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.saveService(ServiceEntity(id = id, name = name.trim(), defaultPrice = price, durationMinutes = duration))
            onDone()
        }
    }

    fun deleteService(service: ServiceEntity) = viewModelScope.launch { repo.deleteService(service) }

    fun saveAppointment(
        id: Long = 0,
        date: LocalDate,
        time: LocalTime,
        clientId: Long,
        serviceId: Long?,
        serviceName: String,
        price: Double,
        notes: String,
        status: AppointmentStatus,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repo.saveAppointment(
                AppointmentEntity(
                    id = id,
                    date = date,
                    time = time,
                    clientId = clientId,
                    serviceId = serviceId,
                    serviceNameSnapshot = serviceName,
                    servicePriceSnapshot = price,
                    notes = notes,
                    status = status
                )
            )
            onDone()
        }
    }

    fun duplicateAppointment(source: AppointmentWithDetails, onDone: () -> Unit = {}) = saveAppointment(
        date = source.date.plusDays(7),
        time = source.time,
        clientId = source.clientId,
        serviceId = source.serviceId,
        serviceName = source.serviceName,
        price = source.price,
        notes = source.notes,
        status = AppointmentStatus.MARCADA,
        onDone = onDone
    )

    fun completeAppointment(source: AppointmentWithDetails) = saveAppointment(
        id = source.id,
        date = source.date,
        time = source.time,
        clientId = source.clientId,
        serviceId = source.serviceId,
        serviceName = source.serviceName,
        price = source.price,
        notes = source.notes,
        status = AppointmentStatus.CONCLUIDA
    )

    fun deleteAppointment(id: Long) = viewModelScope.launch {
        repo.getAppointment(id)?.let { repo.deleteAppointment(it) }
    }
}

class MainViewModelFactory(private val repo: LumiRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repo) as T
    }
}

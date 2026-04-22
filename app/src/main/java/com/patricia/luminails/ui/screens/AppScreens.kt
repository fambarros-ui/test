package com.patricia.luminails.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.patricia.luminails.data.local.ClientEntity
import com.patricia.luminails.data.local.ServiceEntity
import com.patricia.luminails.domain.model.AppointmentStatus
import com.patricia.luminails.domain.model.AppointmentWithDetails
import com.patricia.luminails.ui.navigation.Nav
import com.patricia.luminails.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AppNavHost(nav: NavHostController, vm: MainViewModel) {
    NavHost(navController = nav, startDestination = Nav.Dashboard.route) {
        composable(Nav.Dashboard.route) { DashboardScreen(vm, onGoAppointments = { nav.navigate(Nav.Appointments.route) }, onGoClients = { nav.navigate(Nav.Clients.route) }) }
        composable(Nav.Clients.route) { ClientsScreen(vm) }
        composable(Nav.Appointments.route) { AppointmentsScreen(vm) }
        composable(Nav.Calendar.route) { CalendarScreen(vm) }
        composable(Nav.Services.route) { ServicesScreen(vm) }
        composable(Nav.Settings.route) { SettingsScreen() }
    }
}

@Composable
private fun DashboardScreen(vm: MainViewModel, onGoAppointments: () -> Unit, onGoClients: () -> Unit) {
    val summary by vm.dashboard.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Hoje", style = MaterialTheme.typography.headlineMedium) }
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("Marcações de hoje: ${summary.todayCount}")
                Text("Próxima cliente: ${summary.nextClient ?: "Sem próximas"}")
                Text("Faturação dia: €${"%.2f".format(summary.dayRevenue)}")
                Text("Faturação mês: €${"%.2f".format(summary.monthRevenue)}")
            } }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGoAppointments, modifier = Modifier.weight(1f)) { Text("Nova marcação") }
            Button(onClick = onGoClients, modifier = Modifier.weight(1f)) { Text("Nova cliente") }
        } }
        item { Text("Próximas marcações", style = MaterialTheme.typography.titleMedium) }
        items(summary.upcoming) { AppointmentRow(it, onComplete = { vm.completeAppointment(it) }, onDuplicate = { vm.duplicateAppointment(it) }, onDelete = { vm.deleteAppointment(it.id) }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientsScreen(vm: MainViewModel) {
    val clients by vm.clients.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ClientEntity?>(null) }
    val clipboard = LocalClipboardManager.current
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snack) }, floatingActionButton = {
        FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, null) }
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(vm.clientQuery.value, { vm.clientQuery.value = it }, label = { Text("Pesquisar cliente") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(clients) { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium)
                            if (c.phone.isNotBlank()) Text(c.phone)
                            if (c.notes.isNotBlank()) Text(c.notes, style = MaterialTheme.typography.bodySmall)
                            Row {
                                if (c.phone.isNotBlank()) {
                                    IconButton(onClick = { clipboard.setText(AnnotatedString(c.phone)); scope.launch { snack.showSnackbar("Telefone copiado") } }) { Icon(Icons.Default.ContentCopy, null) }
                                    IconButton(onClick = { scope.launch { snack.showSnackbar("Use o telefone copiado para ligar") } }) { Icon(Icons.Default.Phone, null) }
                                }
                                IconButton(onClick = { editing = c; showDialog = true }) { Icon(Icons.Default.Edit, null) }
                                IconButton(onClick = { vm.deleteClient(c) }) { Icon(Icons.Default.Delete, null) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) ClientDialog(editing, onDismiss = { showDialog = false }) { id, name, phone, notes ->
        vm.saveClient(id, name, phone, notes) { showDialog = false }
    }
}

@Composable
private fun AppointmentsScreen(vm: MainViewModel) {
    val appointments by vm.appointments.collectAsState()
    val clients by vm.clients.collectAsState()
    val services by vm.services.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AppointmentWithDetails?>(null) }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, null) }
    }) { p ->
        Column(Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(vm.appointmentQuery.value, { vm.appointmentQuery.value = it }, label = { Text("Pesquisar marcação") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            StatusFilter(vm)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(appointments) { a ->
                    AppointmentRow(a,
                        onComplete = { vm.completeAppointment(a) },
                        onDuplicate = { vm.duplicateAppointment(a) },
                        onDelete = { vm.deleteAppointment(a.id) },
                        onEdit = { editing = a; showDialog = true })
                }
            }
        }
    }

    if (showDialog) AppointmentDialog(editing, clients, services, onDismiss = { showDialog = false }) { payload ->
        vm.saveAppointment(
            id = payload.id,
            date = payload.date,
            time = payload.time,
            clientId = payload.clientId,
            serviceId = payload.serviceId,
            serviceName = payload.serviceName,
            price = payload.price,
            notes = payload.notes,
            status = payload.status
        ) { showDialog = false }
    }
}

@Composable
private fun StatusFilter(vm: MainViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = { vm.appointmentStatusFilter.value = null }, label = { Text("Todos") })
        AppointmentStatus.entries.forEach { s -> AssistChip(onClick = { vm.appointmentStatusFilter.value = s }, label = { Text(s.name) }) }
    }
}

@Composable
private fun CalendarScreen(vm: MainViewModel) {
    val date by vm.selectedDate.collectAsState()
    val daily by vm.dailyAppointments.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.selectedDate.value = date.minusDays(1) }) { Text("Dia anterior") }
            Button(onClick = { vm.selectedDate.value = date.plusDays(1) }) { Text("Dia seguinte") }
        }
        Text("Agenda de $date", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(daily) { a -> AppointmentRow(a, onComplete = { vm.completeAppointment(a) }, onDuplicate = { vm.duplicateAppointment(a) }, onDelete = { vm.deleteAppointment(a.id) }) }
        }
    }
}

@Composable
private fun ServicesScreen(vm: MainViewModel) {
    val services by vm.services.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ServiceEntity?>(null) }

    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { editing = null; showDialog = true }) { Icon(Icons.Default.Add, null) } }) { p ->
        LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(services) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name)
                            Text("€${"%.2f".format(s.defaultPrice)}")
                            Text("${s.durationMinutes ?: 0} min")
                        }
                        IconButton(onClick = { editing = s; showDialog = true }) { Icon(Icons.Default.Edit, null) }
                        IconButton(onClick = { vm.deleteService(s) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }

    if (showDialog) ServiceDialog(editing, onDismiss = { showDialog = false }) { id, name, price, duration ->
        vm.saveService(id, name, price, duration) { showDialog = false }
    }
}

@Composable
private fun SettingsScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Definições", style = MaterialTheme.typography.headlineMedium)
        Text("• Modo offline ativo")
        Text("• Estrutura preparada para futura sincronização Firebase")
        Text("• Exportação preparada para evolução futura")
    }
}

@Composable
private fun AppointmentRow(
    a: AppointmentWithDetails,
    onComplete: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Card(Modifier.fillMaxWidth().background(if (a.date == LocalDate.now()) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text("${a.date} · ${a.time} · ${a.clientName}", style = MaterialTheme.typography.titleMedium)
            Text("${a.serviceName} · €${"%.2f".format(a.price)} · ${a.status}")
            if (a.notes.isNotBlank()) Text(a.notes, style = MaterialTheme.typography.bodySmall)
            Row {
                IconButton(onClick = onComplete) { Icon(Icons.Default.Check, null) }
                IconButton(onClick = onDuplicate) { Icon(Icons.Default.DateRange, null) }
                onEdit?.let { IconButton(onClick = it) { Icon(Icons.Default.Edit, null) } }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) }
            }
        }
    }
}

@Composable
private fun ClientDialog(editing: ClientEntity?, onDismiss: () -> Unit, onSave: (Long, String, String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(editing?.phone ?: "") }
    var notes by rememberSaveable { mutableStateOf(editing?.notes ?: "") }
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(editing?.id ?: 0, name, phone, notes) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (editing == null) "Nova cliente" else "Editar cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome*") })
                OutlinedTextField(phone, { phone = it }, label = { Text("Telefone") })
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas") })
            }
        })
}

private data class AppointmentPayload(
    val id: Long,
    val date: LocalDate,
    val time: LocalTime,
    val clientId: Long,
    val serviceId: Long?,
    val serviceName: String,
    val price: Double,
    val notes: String,
    val status: AppointmentStatus
)

@Composable
private fun AppointmentDialog(
    editing: AppointmentWithDetails?,
    clients: List<ClientEntity>,
    services: List<ServiceEntity>,
    onDismiss: () -> Unit,
    onSave: (AppointmentPayload) -> Unit
) {
    var date by rememberSaveable { mutableStateOf((editing?.date ?: LocalDate.now()).toString()) }
    var time by rememberSaveable { mutableStateOf((editing?.time ?: LocalTime.of(10, 0)).toString()) }
    var clientId by rememberSaveable { mutableStateOf(editing?.clientId ?: clients.firstOrNull()?.id ?: 0) }
    var serviceId by rememberSaveable { mutableStateOf(editing?.serviceId ?: services.firstOrNull()?.id) }
    var serviceName by rememberSaveable { mutableStateOf(editing?.serviceName ?: services.firstOrNull()?.name.orEmpty()) }
    var price by rememberSaveable { mutableStateOf((editing?.price ?: services.firstOrNull()?.defaultPrice ?: 0.0).toString()) }
    var notes by rememberSaveable { mutableStateOf(editing?.notes ?: "") }
    var status by rememberSaveable { mutableStateOf(editing?.status ?: AppointmentStatus.MARCADA) }
    var serviceExpanded by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = {
            onSave(AppointmentPayload(
                id = editing?.id ?: 0,
                date = LocalDate.parse(date),
                time = LocalTime.parse(time),
                clientId = clientId,
                serviceId = serviceId,
                serviceName = serviceName,
                price = price.toDoubleOrNull() ?: 0.0,
                notes = notes,
                status = status
            ))
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (editing == null) "Nova marcação" else "Editar marcação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(date, { date = it }, label = { Text("Data YYYY-MM-DD") })
                OutlinedTextField(time, { time = it }, label = { Text("Hora HH:MM") })
                OutlinedTextField(clientId.toString(), { clientId = it.toLongOrNull() ?: clientId }, label = { Text("ID cliente") })
                Row(Modifier.fillMaxWidth().clickable { serviceExpanded = true }.padding(4.dp)) { Text("Serviço: $serviceName") }
                DropdownMenu(expanded = serviceExpanded, onDismissRequest = { serviceExpanded = false }) {
                    services.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = {
                        serviceId = s.id
                        serviceName = s.name
                        price = s.defaultPrice.toString()
                        serviceExpanded = false
                    }) }
                }
                OutlinedTextField(price, { price = it }, label = { Text("Preço") })
                OutlinedTextField(notes, { notes = it }, label = { Text("Notas") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppointmentStatus.entries.forEach { st -> AssistChip(onClick = { status = st }, label = { Text(st.name) }) }
                }
            }
        })
}

@Composable
private fun ServiceDialog(editing: ServiceEntity?, onDismiss: () -> Unit, onSave: (Long, String, Double, Int?) -> Unit) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var price by rememberSaveable { mutableStateOf((editing?.defaultPrice ?: 0.0).toString()) }
    var duration by rememberSaveable { mutableStateOf(editing?.durationMinutes?.toString() ?: "") }
    AlertDialog(onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(editing?.id ?: 0, name, price.toDoubleOrNull() ?: 0.0, duration.toIntOrNull()) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (editing == null) "Novo serviço" else "Editar serviço") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") })
                OutlinedTextField(price, { price = it }, label = { Text("Preço") })
                OutlinedTextField(duration, { duration = it }, label = { Text("Duração min") })
            }
        })
}

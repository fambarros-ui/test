package com.patricia.luminails.ui.navigation

sealed class Nav(val route: String, val label: String) {
    data object Dashboard : Nav("dashboard", "Início")
    data object Clients : Nav("clients", "Clientes")
    data object Appointments : Nav("appointments", "Marcações")
    data object Calendar : Nav("calendar", "Agenda")
    data object Services : Nav("services", "Serviços")
    data object Settings : Nav("settings", "Definições")
}

val bottomNavItems = listOf(Nav.Dashboard, Nav.Clients, Nav.Appointments, Nav.Calendar, Nav.Services)

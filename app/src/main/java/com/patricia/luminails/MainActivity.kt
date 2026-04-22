package com.patricia.luminails

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.patricia.luminails.data.local.AppDatabase
import com.patricia.luminails.domain.repository.LumiRepository
import com.patricia.luminails.ui.navigation.Nav
import com.patricia.luminails.ui.navigation.bottomNavItems
import com.patricia.luminails.ui.screens.AppNavHost
import com.patricia.luminails.ui.theme.LumiNailsTheme
import com.patricia.luminails.ui.viewmodel.MainViewModel
import com.patricia.luminails.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.get(this)
        val repo = LumiRepository(db.clientDao(), db.serviceDao(), db.appointmentDao())
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModelFactory(repo))
            val nav = rememberNavController()
            val backStack by nav.currentBackStackEntryAsState()
            LumiNailsTheme {
                Scaffold(bottomBar = {
                    NavigationBar {
                        bottomNavItems.forEach {
                            NavigationBarItem(
                                selected = backStack?.destination?.route == it.route,
                                onClick = { nav.navigate(it.route) },
                                icon = { Icon(iconFor(it), null) },
                                label = { Text(it.label) }
                            )
                        }
                    }
                }) { p ->
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(p)) {
                        AppNavHost(nav, vm)
                    }
                }
            }
        }
    }
}

private fun iconFor(nav: Nav): ImageVector = when (nav) {
    Nav.Dashboard -> Icons.Default.Home
    Nav.Clients -> Icons.Default.Groups
    Nav.Appointments -> Icons.Default.Schedule
    Nav.Calendar -> Icons.Default.CalendarMonth
    Nav.Services -> Icons.Default.MedicalServices
    Nav.Settings -> Icons.Default.Home
}

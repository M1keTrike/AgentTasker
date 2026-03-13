package com.agentasker.features.classroom.presentation.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agentasker.core.ui.components.EmptyState
import com.agentasker.core.ui.components.LoadingState
import com.agentasker.features.classroom.data.services.ClassroomAuthService
import com.agentasker.features.classroom.presentation.components.ClassroomTaskCard
import com.agentasker.features.classroom.presentation.components.ConnectClassroomPrompt
import com.agentasker.features.classroom.presentation.viewmodel.ClassroomViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomScreen(
    viewModel: ClassroomViewModel = hiltViewModel(),
    classroomAuthService: ClassroomAuthService
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val authResult = classroomAuthService.handleAuthResponse(result.data!!)
                viewModel.onClassroomConnected(authResult.authorizationCode, authResult.codeVerifier)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Classroom", fontWeight = FontWeight.ExtraBold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading && !uiState.isConnected -> {
                    LoadingState()
                }
                !uiState.isConnected -> {
                    ConnectClassroomPrompt(
                        isConnecting = uiState.isConnecting,
                        onConnectClick = {
                            val intent = classroomAuthService.createAuthIntent()
                            authLauncher.launch(intent)
                        }
                    )
                }
                uiState.isConnected -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadData() }
                    ) {
                        if (uiState.tasks.isEmpty() && !uiState.isLoading) {
                            EmptyState(
                                message = "No hay tareas en Classroom",
                                icon = Icons.Outlined.School
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                // Course filter chips
                                if (uiState.courses.isNotEmpty()) {
                                    item {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = uiState.selectedCourseId == null,
                                                    onClick = { viewModel.loadTasksByCourse(null) },
                                                    label = { Text("Todos") }
                                                )
                                            }
                                            items(
                                                items = uiState.courses,
                                                key = { it.id }
                                            ) { course ->
                                                FilterChip(
                                                    selected = uiState.selectedCourseId == course.id,
                                                    onClick = { viewModel.loadTasksByCourse(course.id) },
                                                    label = { Text(course.name) }
                                                )
                                            }
                                        }
                                    }
                                }

                                items(
                                    items = uiState.tasks,
                                    key = { it.id }
                                ) { task ->
                                    ClassroomTaskCard(task = task)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package id.app.education.ui.screens.absensi

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.app.education.ui.components.AppCard
import id.app.education.ui.components.AppTextField
import id.app.education.ui.components.EmptyState
import id.app.education.ui.components.LoadingState
import id.app.education.ui.theme.Spacing
import id.app.education.viewmodel.AbsensiViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Consolidated Compose port of the legacy `AbsensiFragment` (bottom-nav tab) and `AbsensiActivity`
 * (pushed-from-Dashboard duplicate) — one screen, one ViewModel, two navigation entry points.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsensiScreen(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: AbsensiViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(selectedDate) { viewModel.getSchedules(selectedDate) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Presensi Siswa") },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { viewModel.getSchedules(selectedDate) },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                AppCard {
                    Column {
                        Text(selectedDate, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Jadwal dan status presensi hari ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.size(Spacing.md))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatColumn("Total", schedules.size.toString(), Modifier.weight(1f))
                            StatColumn("Sudah masuk", schedules.count { !it.attendAt.isNullOrBlank() }.toString(), Modifier.weight(1f))
                            StatColumn("Sudah keluar", schedules.count { !it.leaveAt.isNullOrBlank() }.toString(), Modifier.weight(1f))
                        }
                    }
                }

                AppTextField(
                    value = selectedDate,
                    onValueChange = {},
                    label = "Pilih Tanggal",
                    readOnly = true,
                    trailing = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val cal = Calendar.getInstance().apply { set(year, month, day) }
                                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    },
                )

                Text("Jadwal Hari Ini", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                when {
                    loading && schedules.isEmpty() -> LoadingState()
                    schedules.isEmpty() -> EmptyState(
                        title = "Belum ada jadwal",
                        description = "Tidak ada jadwal pada tanggal ini.",
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        schedules.forEach { item ->
                            AttendanceScheduleCard(
                                item = item,
                                onDetailClick = { viewModel.getDetail(it.subjectScheduleId ?: 0, selectedDate) },
                                onActionClick = { schedule ->
                                    val scheduleId = schedule.subjectScheduleId ?: return@AttendanceScheduleCard
                                    if (schedule.attendAt.isNullOrBlank()) {
                                        // TODO: present a password prompt before attending (parity with legacy stub).
                                    } else if (schedule.leaveAt.isNullOrBlank()) {
                                        viewModel.leave(scheduleId)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

package id.app.education.ui.screens.materi

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import id.app.education.dataclass.ResponData.MateriItem
import id.app.education.ui.components.EmptyState
import id.app.education.ui.theme.Spacing
import id.app.education.utils.PreferenceClass
import id.app.education.viewmodel.MateriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MateriScreen(
    onBack: () -> Unit,
    onOpenDetail: (MateriItem) -> Unit,
    onAddMateri: () -> Unit,
    onEditMateri: (MateriItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MateriViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val pref = remember { PreferenceClass(context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)) }
    val isStudent = remember { isStudentRole(context, pref) }

    val materiList by viewModel.materiList.collectAsStateWithLifecycle()
    val studentSubjects by viewModel.studentSubjects.collectAsStateWithLifecycle()
    val teacherSubjects by viewModel.teacherSubjects.collectAsStateWithLifecycle()
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    var selectedSubjectName by remember { mutableStateOf<String?>(null) }
    var selectedClassName by remember { mutableStateOf<String?>(null) }
    var menuTarget by remember { mutableStateOf<MateriItem?>(null) }
    var deleteTarget by remember { mutableStateOf<MateriItem?>(null) }

    LaunchedEffect(Unit) {
        if (isStudent) {
            viewModel.fetchStudentSubjects()
        } else {
            viewModel.fetchTeacherRequirements()
            viewModel.getMateriTeacher()
        }
    }

    LaunchedEffect(studentSubjects) {
        if (isStudent && selectedSubjectName == null && studentSubjects.isNotEmpty()) {
            selectedSubjectName = studentSubjects.first().name
            viewModel.getMateriStudent(studentSubjects.first().id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Materi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isStudent) {
                ExtendedFloatingActionButton(text = { Text("Tambah Materi") }, onClick = onAddMateri, icon = {})
            }
        },
        modifier = modifier,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            PullToRefreshBox(
                isRefreshing = loading,
                onRefresh = { viewModel.refreshMateri(isStudent) },
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (!isStudent) {
                            SimpleDropdown(
                                label = "Kelas",
                                options = listOf("Semua") + classes.map { it.name },
                                selected = selectedClassName ?: "Semua",
                                onSelected = { name ->
                                    selectedClassName = name
                                    val classId = if (name == "Semua") null else classes.find { it.name == name }?.id
                                    val subjectId = teacherSubjects.find { it.name == selectedSubjectName }?.id
                                    viewModel.getMateriTeacher(subjectId = subjectId, classId = classId)
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        SimpleDropdown(
                            label = "Mata Pelajaran",
                            options = if (isStudent) studentSubjects.map { it.name } else listOf("Semua") + teacherSubjects.map { it.name },
                            selected = selectedSubjectName ?: (if (isStudent) "" else "Semua"),
                            onSelected = { name ->
                                selectedSubjectName = name
                                if (isStudent) {
                                    studentSubjects.find { it.name == name }?.let { viewModel.getMateriStudent(it.id) }
                                } else {
                                    val subjectId = if (name == "Semua") null else teacherSubjects.find { it.name == name }?.id
                                    val classId = classes.find { it.name == selectedClassName }?.id
                                    viewModel.getMateriTeacher(subjectId = subjectId, classId = classId)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (materiList.isEmpty() && !loading) {
                        EmptyState(
                            title = "Belum Ada Materi",
                            description = "Materi belum tersedia atau tidak sesuai dengan filter yang dipilih",
                            modifier = Modifier.padding(top = Spacing.huge),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(materiList, key = { it.id }) { item ->
                                MateriListItem(
                                    item = item,
                                    isStudent = isStudent,
                                    onMoreClick = { menuTarget = it },
                                    onItemClick = onOpenDetail,
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = loading && materiList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("loading.json"))
                        val progress by animateLottieCompositionAsState(composition, iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever)
                        LottieAnimation(composition = composition, progress = { progress }, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.size(Spacing.md))
                        Text("Memuat materi...", color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    menuTarget?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { menuTarget = null },
            title = { Text("Opsi Materi") },
            text = { Text(item.name) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onEditMateri(item); menuTarget = null }) { Text("Edit") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteTarget = item; menuTarget = null }) { Text("Hapus") }
            },
        )
    }

    deleteTarget?.let { item ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus Materi") },
            text = { Text("Apakah Anda yakin ingin menghapus materi '${item.name}'?") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteMateri(item.id.toLong(), isStudent)
                    deleteTarget = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}

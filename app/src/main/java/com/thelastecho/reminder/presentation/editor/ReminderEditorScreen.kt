package com.thelastecho.reminder.presentation.editor

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thelastecho.reminder.core.designsystem.ReminderShapes
import com.thelastecho.reminder.data.attachments.AttachmentStore
import com.thelastecho.reminder.core.designsystem.ReminderDimensions
import com.thelastecho.reminder.core.designsystem.PriorityHigh
import com.thelastecho.reminder.core.designsystem.PriorityLow
import com.thelastecho.reminder.core.designsystem.PriorityMedium
import com.thelastecho.reminder.domain.model.Priority
import com.thelastecho.reminder.presentation.components.CategoryIcon
import com.thelastecho.reminder.presentation.components.SectionHeading
import com.thelastecho.reminder.presentation.components.priorityLabelResource
import com.thelastecho.reminder.presentation.components.repeatLabelResource
import com.thelastecho.reminder.domain.model.RepeatInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val attachmentStore = remember(context) { AttachmentStore(context) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var newSubTaskText by remember { mutableStateOf("") }
    var repeatDropdownExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            val copied = runCatching { withContext(Dispatchers.IO) { attachmentStore.copyFromUri(uri) } }
            copied.onSuccess {
                viewModel.onIntent(EditorIntent.SetImageAttachment(it.relativePath))
            }.onFailure {
                snackbarHostState.showSnackbar(context.getString(com.thelastecho.reminder.R.string.photo_copy_failed))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EditorEffect.NavigateBack -> onNavigateBack()
                is EditorEffect.ShowSnackbar -> snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                is EditorEffect.ShowDeleteUndo -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(com.thelastecho.reminder.R.string.reminder_deleted),
                        actionLabel = context.getString(com.thelastecho.reminder.R.string.undo)
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(EditorIntent.UndoDelete(effect.reminderId))
                    }
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.reminderId != null) stringResource(com.thelastecho.reminder.R.string.edit_reminder) else stringResource(com.thelastecho.reminder.R.string.new_reminder),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.thelastecho.reminder.R.string.back))
                    }
                },
                actions = {
                    if (state.reminderId != null) {
                        IconButton(onClick = { viewModel.onIntent(EditorIntent.DeleteReminder) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(com.thelastecho.reminder.R.string.delete))
                        }
                    }
                    TextButton(
                        onClick = { viewModel.onIntent(EditorIntent.SaveReminder) },
                        enabled = !state.isSaving
                    ) {
                        Text(
                            text = stringResource(com.thelastecho.reminder.R.string.save),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ReminderDimensions.ContentMaxWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(ReminderDimensions.Medium),
                verticalArrangement = Arrangement.spacedBy(ReminderDimensions.Medium)
            ) {
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.basic_information))

            // Title Input
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onIntent(EditorIntent.UpdateTitle(it)) },
                label = { Text(stringResource(com.thelastecho.reminder.R.string.what_reminded)) },
                placeholder = { Text(stringResource(com.thelastecho.reminder.R.string.call_doctor)) },
                isError = state.titleErrorRes != null,
                supportingText = state.titleErrorRes?.let { id -> { Text(stringResource(id)) } },
                modifier = Modifier.fillMaxWidth(),
                shape = ReminderShapes.Input
            )

            // Notes / Description Input
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.onIntent(EditorIntent.UpdateNotes(it)) },
                label = { Text(stringResource(com.thelastecho.reminder.R.string.notes_optional)) },
                placeholder = { Text(stringResource(com.thelastecho.reminder.R.string.add_extra_details)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                shape = ReminderShapes.Input
            )

            // Date & Time Scheduling Section
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.schedule_alarm))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    ReminderShapes.Card
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.dueDateTimeEpochMillis == null) {
                        // Quick add date buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.weight(1f),
                                shape = ReminderShapes.Control
                            ) {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(com.thelastecho.reminder.R.string.set_date))
                            }
                        }
                    } else {
                        val zone = ZoneId.systemDefault()
                        val zdt = Instant.ofEpochMilli(state.dueDateTimeEpochMillis!!).atZone(zone)
                        val appLocale = LocalConfiguration.current.locales[0]
                        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale)
                        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(appLocale)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(ReminderShapes.Compact)
                                    .clickable { showDatePicker = true }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(zdt.format(dateFormatter), style = MaterialTheme.typography.bodyLarge)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(ReminderShapes.Compact)
                                    .clickable { showTimePicker = true }
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(zdt.format(timeFormatter), style = MaterialTheme.typography.bodyLarge)
                            }

                            IconButton(onClick = { viewModel.onIntent(EditorIntent.ClearDateTime) }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(com.thelastecho.reminder.R.string.clear_date_time))
                            }
                        }

                        // Recurrence Dropdown
                        Spacer(modifier = Modifier.height(10.dp))
                        ExposedDropdownMenuBox(
                            expanded = repeatDropdownExpanded,
                            onExpandedChange = { repeatDropdownExpanded = !repeatDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = stringResource(repeatLabelResource(state.repeatInterval)),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(com.thelastecho.reminder.R.string.repeat)) },
                                leadingIcon = { Icon(Icons.Outlined.Repeat, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = ReminderShapes.Control
                            )
                            ExposedDropdownMenu(
                                expanded = repeatDropdownExpanded,
                                onDismissRequest = { repeatDropdownExpanded = false }
                            ) {
                                RepeatInterval.entries.forEach { interval ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(repeatLabelResource(interval))) },
                                        onClick = {
                                            viewModel.onIntent(EditorIntent.SetRepeatInterval(interval))
                                            repeatDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Categories Selector
            if (state.categories.isNotEmpty()) {
                SectionHeading(stringResource(com.thelastecho.reminder.R.string.category))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = state.categoryId == null,
                        onClick = { viewModel.onIntent(EditorIntent.SetCategory(null)) },
                        label = { Text(stringResource(com.thelastecho.reminder.R.string.no_category)) }
                    )
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.onIntent(EditorIntent.SetCategory(category.id)) },
                            label = { Text(category.name) },
                            leadingIcon = { CategoryIcon(category.iconName, Modifier.size(18.dp), androidx.compose.ui.graphics.Color(category.colorArgb)) }
                        )
                    }
                }
            }

            // Checklist / Subtasks Section
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.subtasks_checklist))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    ReminderShapes.Card
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    state.subTasks.forEachIndexed { index, subTask ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.onIntent(EditorIntent.ToggleSubTask(index)) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (subTask.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (subTask.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subTask.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onIntent(EditorIntent.DeleteSubTask(index)) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = stringResource(com.thelastecho.reminder.R.string.delete_subtask), modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Add Subtask row
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubTaskText,
                            onValueChange = { newSubTaskText = it },
                            placeholder = { Text(stringResource(com.thelastecho.reminder.R.string.add_subtask)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = ReminderShapes.Input
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newSubTaskText.isNotBlank()) {
                                    viewModel.onIntent(EditorIntent.AddSubTask(newSubTaskText))
                                    newSubTaskText = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(com.thelastecho.reminder.R.string.add))
                        }
                    }
                }
            }

            var photoBitmap by remember(state.imagePath, state.legacyImageUri) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(state.imagePath, state.legacyImageUri) {
                if (!state.imagePath.isNullOrBlank() || !state.legacyImageUri.isNullOrBlank()) {
                    photoBitmap = withContext(Dispatchers.IO) {
                        runCatching {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            attachmentStore.open(state.imagePath, state.legacyImageUri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                            val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
                            val sampleSize = if (maxDimension > 1280) (maxDimension / 1280).coerceAtLeast(1) else 1
                            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                            attachmentStore.open(state.imagePath, state.legacyImageUri)?.use { BitmapFactory.decodeStream(it, null, options) }
                        }.getOrNull()
                    }
                } else {
                    photoBitmap = null
                }
            }

            // Image Attachment via System PhotoPicker
            SectionHeading(stringResource(com.thelastecho.reminder.R.string.attachment))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    ReminderShapes.Card
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (state.imagePath != null || state.legacyImageUri != null) stringResource(com.thelastecho.reminder.R.string.photo_attached) else stringResource(com.thelastecho.reminder.R.string.add_photo),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    if (state.imagePath != null || state.legacyImageUri != null) {
                        IconButton(onClick = { viewModel.onIntent(EditorIntent.SetImageAttachment(null)) }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(com.thelastecho.reminder.R.string.remove_photo))
                        }
                    } else {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = ReminderShapes.Control
                        ) {
                            Text(stringResource(com.thelastecho.reminder.R.string.select))
                        }
                    }
                }
                if (photoBitmap != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = photoBitmap!!.asImageBitmap(),
                        contentDescription = stringResource(com.thelastecho.reminder.R.string.photo_attached),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(ReminderShapes.Control)
                    )
                }
                }
            }

            SectionHeading(stringResource(com.thelastecho.reminder.R.string.advanced_options))

            Card(
                shape = ReminderShapes.Card,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth().border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    ReminderShapes.Card
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(com.thelastecho.reminder.R.string.priority),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Priority.entries.forEach { priority ->
                            val isSelected = state.priority == priority
                            val chipColor = when (priority) {
                                Priority.HIGH -> PriorityHigh
                                Priority.MEDIUM -> PriorityMedium
                                Priority.LOW -> PriorityLow
                                Priority.NONE -> MaterialTheme.colorScheme.outline
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onIntent(EditorIntent.SetPriority(priority)) },
                                label = { Text(stringResource(priorityLabelResource(priority))) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                    selectedLabelColor = chipColor
                                )
                            )
                        }
                    }

                    Text(
                        stringResource(com.thelastecho.reminder.R.string.notification_style),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            null to stringResource(com.thelastecho.reminder.R.string.notification_default),
                            "SIMPLE" to stringResource(com.thelastecho.reminder.R.string.simple_style_name),
                            "HEADS_UP" to stringResource(com.thelastecho.reminder.R.string.heads_up_style_name),
                            "FULL_SCREEN" to stringResource(com.thelastecho.reminder.R.string.full_screen_style_name)
                        ).forEach { (style, label) ->
                            FilterChip(
                                selected = state.notificationStyle == style,
                                onClick = { viewModel.onIntent(EditorIntent.SetNotificationStyle(style)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dueDateTimeEpochMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onIntent(EditorIntent.SetDueDate(it))
                    }
                    showDatePicker = false
                    if (state.dueDateTimeEpochMillis == null) {
                        showTimePicker = true
                    }
                }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 TimePickerDialog
    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val initialTime = if (state.dueDateTimeEpochMillis != null) {
            Instant.ofEpochMilli(state.dueDateTimeEpochMillis!!).atZone(zone).toLocalTime()
        } else {
            LocalTime.of(9, 0)
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onIntent(EditorIntent.SetDueTime(timePickerState.hour, timePickerState.minute))
                    showTimePicker = false
                }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(com.thelastecho.reminder.R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

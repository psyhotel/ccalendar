package com.example.memories.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // EDIT: add state helpers
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.memories.db.ThoughtWithReport
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast
import java.text.DateFormat
import java.util.Date
import android.provider.CalendarContract
import android.content.ContentValues
import android.content.ContentUris
import java.util.TimeZone

// ADD: permission helpers
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

// ADD: activity result launcher for runtime permissions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ThoughtDetailsScreen(
    item: ThoughtWithReport?,
    onUpdate: (ThoughtUpdate) -> Unit = {},
    onDelete: (Long) -> Unit = {}
) {
    if (item == null) {
        Text("Not found")
        return
    }
    val context = LocalContext.current

    // EDIT: edit/delete dialog state
    var showEdit by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // ADD: runtime permission launcher for calendar
    val calendarPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = (grants[Manifest.permission.WRITE_CALENDAR] == true) &&
                      (grants[Manifest.permission.READ_CALENDAR] == true)
        Toast.makeText(
            context,
            if (granted) "Calendar permissions granted" else "Calendar permissions denied",
            Toast.LENGTH_SHORT
        ).show()
    }

    Column(Modifier.padding(16.dp)) {
        Text(item.thought.title, style = MaterialTheme.typography.titleLarge)
        Text(item.thought.category, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Text("Summary")
        Text(item.thought.summary)
        Spacer(Modifier.height(8.dp))

        // NEW: show due date if available
        if (item.thought.dueAt != null) {
            val formatted = DateFormat.getDateTimeInstance().format(Date(item.thought.dueAt!!))
            Text("Due: $formatted", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        Text("Details")
        Text(item.thought.fullText)
        Spacer(Modifier.height(8.dp))
        Text("Report")
        Text(item.report?.content ?: "No report")

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // NEW: Add to Calendar (permission-aware)
            Button(
                enabled = item.thought.dueAt != null,
                onClick = {
                    val due = item.thought.dueAt
                    if (due != null) {
                        val hasWrite = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.WRITE_CALENDAR
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasRead = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.READ_CALENDAR
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasWrite || !hasRead) {
                            // ASK: request permissions, then user can press again
                            calendarPermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.WRITE_CALENDAR,
                                    Manifest.permission.READ_CALENDAR
                                )
                            )
                            return@Button
                        }

                        val eventId = createCalendarEvent(
                            context = context,
                            title = item.thought.title,
                            description = item.thought.summary,
                            startMillis = due
                        )
                        Toast.makeText(
                            context,
                            if (eventId != null) "Event created" else "Failed to create event",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(context, "No due date", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Add to Calendar") }

            // ADD: Open Calendar editor (no direct write permission required)
            Button(
                enabled = item.thought.dueAt != null,
                onClick = {
                    val due = item.thought.dueAt
                    if (due != null) {
                        openCalendarEditor(
                            context = context,
                            title = item.thought.title,
                            description = item.thought.summary,
                            startMillis = due
                        )
                    } else {
                        Toast.makeText(context, "No due date", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("Open Calendar editor") }

            // NEW: Share report
            Button(onClick = {
                val textToShare = buildString {
                    appendLine(item.thought.title)
                    appendLine(item.thought.category)
                    appendLine()
                    appendLine("Summary:")
                    appendLine(item.thought.summary)
                    appendLine()
                    appendLine("Details:")
                    appendLine(item.thought.fullText)
                    if (item.report?.content != null) {
                        appendLine()
                        appendLine("Report:")
                        appendLine(item.report!!.content)
                    }
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, textToShare)
                }
                context.startActivity(Intent.createChooser(intent, "Share thought"))
            }) { Text("Share") }

            // NEW: Copy summary
            Button(onClick = {
                android.content.ClipboardManager::class.java
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Summary", item.thought.summary))
                Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
            }) { Text("Copy Summary") }

            // ADD: Copy Details
            Button(onClick = {
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Details", item.thought.fullText))
                Toast.makeText(context, "Details copied", Toast.LENGTH_SHORT).show()
            }) { Text("Copy Details") }

            // ADD: Copy Report
            Button(onClick = {
                val report = item.report?.content
                if (report.isNullOrBlank()) {
                    Toast.makeText(context, "No report to copy", Toast.LENGTH_SHORT).show()
                } else {
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Report", report))
                    Toast.makeText(context, "Report copied", Toast.LENGTH_SHORT).show()
                }
            }) { Text("Copy Report") }
        }

        // EDIT: edit/delete actions
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showEdit = true }) { Text("Edit") }
            Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = { showDeleteConfirm = true }) { Text("Delete") }
        }
    }

    // EDIT: Edit Dialog
    if (showEdit) {
        var title by remember(item.thought.id) { mutableStateOf(item.thought.title) }
        var category by remember(item.thought.id) { mutableStateOf(item.thought.category) }
        var summary by remember(item.thought.id) { mutableStateOf(item.thought.summary) }
        var details by remember(item.thought.id) { mutableStateOf(item.thought.fullText) }
        var hasDue by remember(item.thought.id) { mutableStateOf(item.thought.dueAt != null) }
        var dueMillisText by remember(item.thought.id) {
            mutableStateOf(item.thought.dueAt?.toString() ?: "")
        }

        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit Thought") },
            text = {
                Column {
                    TextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    Spacer(Modifier.height(8.dp))
                    TextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                    Spacer(Modifier.height(8.dp))
                    TextField(value = summary, onValueChange = { summary = it }, label = { Text("Summary") })
                    Spacer(Modifier.height(8.dp))
                    TextField(value = details, onValueChange = { details = it }, label = { Text("Details") })
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Checkbox(checked = hasDue, onCheckedChange = {
                            hasDue = it
                            if (!it) dueMillisText = ""
                        })
                        Spacer(Modifier.width(8.dp))
                        Text("Has due date (ms)")
                    }
                    if (hasDue) {
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = dueMillisText,
                            onValueChange = { dueMillisText = it },
                            label = { Text("Due time millis") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dueAt = if (hasDue) dueMillisText.toLongOrNull() else null
                    onUpdate(
                        ThoughtUpdate(
                            id = item.thought.id,
                            title = title,
                            category = category,
                            summary = summary,
                            fullText = details,
                            dueAt = dueAt
                        )
                    )
                    showEdit = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }

    // EDIT: Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Thought") },
            text = { Text("This will remove the thought, its report, transcription, recording, and audio file. Proceed?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item.thought.id)
                    showDeleteConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

// NEW: helper to create calendar event
private fun createCalendarEvent(
    context: android.content.Context,
    title: String,
    description: String,
    startMillis: Long
): Long? {
    val values = ContentValues().apply {
        put(CalendarContract.Events.DTSTART, startMillis)
        put(CalendarContract.Events.DTEND, startMillis + 60 * 60 * 1000)
        put(CalendarContract.Events.TITLE, title)
        put(CalendarContract.Events.DESCRIPTION, description)
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        // Calendar ID: try to use primary or fallback to first available
    }

    val calId = run {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        var result: Long? = null
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            var fallback: Long? = null
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val isPrimary = cursor.getInt(1) == 1
                if (isPrimary) {
                    result = id
                    break
                }
                if (fallback == null) fallback = id
            }
            if (result == null) result = fallback
        }
        result
    } ?: return null

    values.put(CalendarContract.Events.CALENDAR_ID, calId)

    val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return null
    val eventId = ContentUris.parseId(uri)

    val reminderValues = ContentValues().apply {
        put(CalendarContract.Reminders.EVENT_ID, eventId)
        put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        put(CalendarContract.Reminders.MINUTES, 10)
    }
    context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)

    return eventId
}

// ADD: helper to open Calendar editor with prefilled event (works without calendar permissions)
private fun openCalendarEditor(
    context: android.content.Context,
    title: String,
    description: String,
    startMillis: Long
) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 60 * 60 * 1000)
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
    }
    context.startActivity(intent)
}

data class ThoughtUpdate(
    val id: Long,
    val title: String,
    val category: String,
    val summary: String,
    val fullText: String,
    val dueAt: Long?
)
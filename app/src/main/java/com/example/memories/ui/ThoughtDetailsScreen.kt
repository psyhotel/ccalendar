package com.example.memories.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun ThoughtDetailsScreen(item: ThoughtWithReport?) {
    if (item == null) {
        Text("Not found")
        return
    }
    val context = LocalContext.current
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
            // NEW: Add to Calendar
            Button(
                enabled = item.thought.dueAt != null,
                onClick = {
                    val due = item.thought.dueAt
                    if (due != null) {
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
        }
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
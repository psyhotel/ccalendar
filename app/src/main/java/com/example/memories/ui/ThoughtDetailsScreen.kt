package com.example.memories.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.memories.db.ThoughtWithReport

@Composable
fun ThoughtDetailsScreen(item: ThoughtWithReport?) {
    if (item == null) {
        Text("Not found")
        return
    }
    Column(Modifier.padding(16.dp)) {
        Text(item.thought.title, style = MaterialTheme.typography.titleLarge)
        Text(item.thought.category, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Text("Summary")
        Text(item.thought.summary)
        Spacer(Modifier.height(8.dp))
        Text("Details")
        Text(item.thought.fullText)
        Spacer(Modifier.height(8.dp))
        Text("Report")
        Text(item.report?.content ?: "No report")
    }
}
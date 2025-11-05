package com.example.memories.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.memories.db.ThoughtWithReport

@Composable
fun MainScreen(vm: MainViewModel) {
    val isRecording by vm.isRecording.collectAsState()
    val thoughts by vm.thoughts.collectAsState()

    var hasAudioPermission by remember { mutableStateOf(false) }
    val permissions = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_CALENDAR)
        add(Manifest.permission.WRITE_CALENDAR)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasAudioPermission = result[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        vm.refreshThoughts()
    }

    Column(Modifier.padding(16.dp)) {
        Text("Voice Thoughts", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        if (!hasAudioPermission) {
            Button(onClick = { launcher.launch(permissions.toTypedArray()) }) {
                Text("Grant Permissions")
            }
        } else {
            if (!isRecording) {
                Button(onClick = { vm.startRecording() }) { Text("Start Recording") }
            } else {
                Button(onClick = {
                    vm.stopRecordingAndProcess()
                    // refresh after background work finishes - you could observe WorkManager or poll
                    vm.refreshThoughts()
                }) { Text("Stop & Process") }
            }
        }

        Spacer(Modifier.height(24.dp))
        ThoughtsList(thoughts = thoughts, onSelect = { /* navigate to details */ })
    }
}

@Composable
fun ThoughtsList(thoughts: List<ThoughtWithReport>, onSelect: (Long) -> Unit) {
    LazyColumn {
        items(thoughts) { item ->
            ThoughtRow(item, onSelect)
            Divider()
        }
    }
}

@Composable
fun ThoughtRow(item: ThoughtWithReport, onSelect: (Long) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onSelect(item.thought.id) }
            .padding(vertical = 8.dp)
    ) {
        Text(item.thought.title, style = MaterialTheme.typography.titleMedium)
        Text(item.thought.category, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Text(item.thought.summary, maxLines = 3)
    }
}
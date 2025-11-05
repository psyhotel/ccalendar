package com.example.memories.repo

import com.example.memories.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ThoughtRepository(private val dao: ThoughtDao) {

    suspend fun savePipelineResult(
        recordingPath: String,
        transcriptionText: String,
        title: String,
        category: String,
        summary: String,
        fullText: String,
        dueAt: Long?,
        reportContent: String
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val recId = dao.insertRecording(RecordingEntity(filePath = recordingPath, createdAt = now))
        val trId = dao.insertTranscription(TranscriptionEntity(recordingId = recId, text = transcriptionText))
        val thoughtId = dao.insertThought(
            ThoughtEntity(
                transcriptionId = trId,
                title = title,
                category = category,
                summary = summary,
                fullText = fullText,
                dueAt = dueAt,
                createdAt = now
            )
        )
        dao.insertReport(ReportEntity(thoughtId = thoughtId, content = reportContent, createdAt = now))
        thoughtId
    }

    suspend fun listThoughts(): List<ThoughtWithReport> = withContext(Dispatchers.IO) {
        dao.getThoughtsWithReports()
    }

    suspend fun getThought(id: Long): ThoughtWithReport? = withContext(Dispatchers.IO) {
        dao.getThoughtWithReport(id)
    }

    // ADD: update thought fields
    suspend fun updateThoughtFields(
        thoughtId: Long,
        title: String,
        category: String,
        summary: String,
        fullText: String,
        dueAt: Long?
    ) = withContext(Dispatchers.IO) {
        val current = dao.getThoughtById(thoughtId) ?: return@withContext
        val updated = current.copy(
            title = title,
            category = category,
            summary = summary,
            fullText = fullText,
            dueAt = dueAt
        )
        dao.updateThought(updated)
    }

    // ADD: delete thought + transcription + recording + audio file
    suspend fun deleteThoughtCascade(thoughtId: Long) = withContext(Dispatchers.IO) {
        val thought = dao.getThoughtById(thoughtId) ?: return@withContext
        val transcription = dao.getTranscriptionById(thought.transcriptionId)
        val recording = transcription?.let { dao.getRecordingById(it.recordingId) }

        // delete transcription; CASCADE will remove the thought and its report
        transcription?.let { dao.deleteTranscriptionById(it.id) }

        // delete recording db row
        recording?.let { dao.deleteRecordingById(it.id) }

        // delete audio file from storage
        recording?.filePath?.let { path ->
            runCatching { File(path).takeIf { it.exists() }?.delete() }
        }
    }
}
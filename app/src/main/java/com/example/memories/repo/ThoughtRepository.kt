package com.example.memories.repo

import com.example.memories.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
}
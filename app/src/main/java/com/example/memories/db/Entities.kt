package com.example.memories.db

import androidx.room.*

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val createdAt: Long
)

@Entity(
    tableName = "transcriptions",
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recordingId")]
)
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val text: String
)

@Entity(
    tableName = "thoughts",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transcriptionId")]
)
data class ThoughtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcriptionId: Long,
    val title: String,
    val category: String,
    val summary: String,
    val fullText: String,
    val dueAt: Long?,
    val createdAt: Long
)

@Entity(
    tableName = "reports",
    foreignKeys = [
        ForeignKey(
            entity = ThoughtEntity::class,
            parentColumns = ["id"],
            childColumns = ["thoughtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("thoughtId")]
)
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val thoughtId: Long,
    val content: String,
    val createdAt: Long
)

data class ThoughtWithReport(
    @Embedded val thought: ThoughtEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "thoughtId"
    )
    val report: ReportEntity?
)

@Dao
interface ThoughtDao {
    @Insert
    suspend fun insertRecording(r: RecordingEntity): Long

    @Insert
    suspend fun insertTranscription(t: TranscriptionEntity): Long

    @Insert
    suspend fun insertThought(t: ThoughtEntity): Long

    @Insert
    suspend fun insertReport(r: ReportEntity): Long

    @Transaction
    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC")
    suspend fun getThoughtsWithReports(): List<ThoughtWithReport>

    @Transaction
    @Query("SELECT * FROM thoughts WHERE id = :id")
    suspend fun getThoughtWithReport(id: Long): ThoughtWithReport?

    // ADD: fetch single entities for update/delete cascade
    @Query("SELECT * FROM thoughts WHERE id = :id")
    suspend fun getThoughtById(id: Long): ThoughtEntity?

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getTranscriptionById(id: Long): TranscriptionEntity?

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    // ADD: update thought
    @Update
    suspend fun updateThought(t: ThoughtEntity)

    // ADD: delete chain parts
    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteTranscriptionById(id: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)
}
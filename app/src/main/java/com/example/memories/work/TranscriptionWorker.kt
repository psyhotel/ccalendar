class TranscriptionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val audioPath = inputData.getString("audio_path") ?: return Result.failure()
        val transcription = uploadAndTranscribe(audioPath) ?: return Result.retry()

        val structured = categorizeAndStructure(transcription)

        structured.dueAt?.let {
            CalendarIntegration.createEvent(applicationContext, structured.title, structured.summary, it)
        }

        val report = ReportGenerator.generate(structured)

        // NEW: persist via Room repository
        val db = AppDatabase.get(applicationContext)
        val repo = ThoughtRepository(db.thoughtDao())
        repo.savePipelineResult(
            recordingPath = audioPath,
            transcriptionText = transcription,
            title = structured.title,
            category = structured.category,
            summary = structured.summary,
            fullText = structured.fullText,
            dueAt = structured.dueAt,
            reportContent = report
        )

        return Result.success()
    }

    private suspend fun uploadAndTranscribe(audioPath: String): String? {
        // Replace with your API call. Example using OkHttp:
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        val file = File(audioPath)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("audio/m4a".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("https://your-backend.example.com/transcribe")
            .post(body)
            .build()

        return client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            // Expect JSON: { "text": "..." }
            val json = resp.body?.string() ?: return@use null
            JSONObject(json).optString("text", null)
        }
    }

    private fun categorizeAndStructure(text: String): StructuredThought {
        // Placeholder: replace with LLM call to your backend for better results
        val title = text.split("\n").firstOrNull()?.take(60) ?: "Thought"
        val category = when {
            text.contains("идея", ignoreCase = true) -> "Business Idea"
            text.contains("встреч", ignoreCase = true) -> "Meeting"
            text.contains("задач", ignoreCase = true) -> "Task"
            else -> "General"
        }
        val summary = summarize(text)
        val dueAt = extractDueDateMillis(text) // naive extraction; replace with NLP
        return StructuredThought(title = title, category = category, summary = summary, fullText = text, dueAt = dueAt)
    }

    private fun summarize(text: String): String {
        return text.lines().take(5).joinToString(" ")
    }

    private fun extractDueDateMillis(text: String): Long? {
        // Very naive example: look for pattern "завтра в HH:MM"
        val regex = Regex("""завтра в (\d{1,2}):(\d{2})""", RegexOption.IGNORE_CASE)
        val match = regex.find(text) ?: return null
        val (h, m) = match.destructured
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, h.toInt())
            set(Calendar.MINUTE, m.toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

data class StructuredThought(
    val title: String,
    val category: String,
    val summary: String,
    val fullText: String,
    val dueAt: Long?
)
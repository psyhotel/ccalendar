package com.example.memories.ui

class MainViewModel(private val appContext: android.content.Context) : androidx.lifecycle.ViewModel() {
    private val recorder = AudioRecorder()
    private val db = AppDatabase.get(appContext)
    private val repo = ThoughtRepository(db.thoughtDao())

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _thoughts = MutableStateFlow<List<ThoughtWithReport>>(emptyList())
    val thoughts = _thoughts.asStateFlow()

    fun refreshThoughts() {
        viewModelScope.launch {
            _thoughts.value = repo.listThoughts()
        }
    }

    fun startRecording() {
        recorder.start(appContext)
        _isRecording.value = true
    }

    fun stopRecordingAndProcess() {
        val path = recorder.stop() ?: return
        _isRecording.value = false

        val work = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf("audio_path" to path))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(appContext).enqueue(work)
    }

    // ADD: update thought
    fun updateThought(
        thoughtId: Long,
        title: String,
        category: String,
        summary: String,
        fullText: String,
        dueAt: Long?
    ) {
        viewModelScope.launch {
            repo.updateThoughtFields(thoughtId, title, category, summary, fullText, dueAt)
            refreshThoughts()
        }
    }

    // ADD: delete thought
    fun deleteThought(thoughtId: Long) {
        viewModelScope.launch {
            repo.deleteThoughtCascade(thoughtId)
            refreshThoughts()
        }
    }
}
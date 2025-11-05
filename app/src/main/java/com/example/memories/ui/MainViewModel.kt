class MainViewModel(private val appContext: Context) : ViewModel() {
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
}
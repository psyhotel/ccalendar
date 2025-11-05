class AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFilePath: String? = null

    fun start(context: Context): String {
        val fileName = "rec_${System.currentTimeMillis()}.m4a"
        val file = File(context.getExternalFilesDir(null), fileName)
        outputFilePath = file.absolutePath

        val r = MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(outputFilePath)
        r.prepare()
        r.start()
        recorder = r
        return outputFilePath!!
    }

    fun stop(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFilePath
        } catch (e: Exception) {
            recorder = null
            null
        }
    }
}
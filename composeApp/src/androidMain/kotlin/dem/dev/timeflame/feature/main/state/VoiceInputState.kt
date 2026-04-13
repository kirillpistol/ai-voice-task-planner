package dem.dev.timeflame.feature.main.state

enum class RecordingState {
    Idle, Recording, RecordingFinished
}

data class VoiceInputState(
    val recordingState: RecordingState = RecordingState.Idle,
    val currentRecognizedText: String = ""
)
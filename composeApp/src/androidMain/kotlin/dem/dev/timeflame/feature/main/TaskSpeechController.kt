package dem.dev.timeflame.feature.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dem.dev.timeflame.util.state.UiMessageCodes

internal fun voiceErrorMessage(error: Int): Int = when (error) {
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> UiMessageCodes.voicePermissionDenied
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> UiMessageCodes.voiceNetworkFailed
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> UiMessageCodes.voiceNoSpeech
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY, SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> UiMessageCodes.voiceBusy
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> UiMessageCodes.voiceLanguageUnavailable
    SpeechRecognizer.ERROR_AUDIO -> UiMessageCodes.voiceAudioFailed
    else -> UiMessageCodes.voiceRecognitionFailed
}

/** Main-thread owner of one recognizer per recording, including watchdog and cleanup. */
internal class TaskSpeechController(
    private val context: Context,
    private val onProcessing: () -> Unit,
    private val onRecognized: (String) -> Unit,
    private val onFailure: (Int) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val gate = VoiceSessionGate()
    private var recognizer: SpeechRecognizer? = null
    private var watchdog: Runnable? = null

    fun start(language: String) {
        cancel()
        val session = gate.open()
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                fail(session, UiMessageCodes.voiceUnavailable)
                return
            }
            val current = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = current
            current.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (gate.accepts(session)) Log.d("GenesisVoice", "Recognition ready")
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    if (!gate.accepts(session)) return
                    armTimeout(session, 20_000L)
                    onProcessing()
                }
                override fun onError(error: Int) {
                    if (!gate.accepts(session)) return
                    // Diagnostics contain the error code, never audio or recognized text.
                    Log.w("GenesisVoice", "Recognition error=$error")
                    fail(session, voiceErrorMessage(error))
                }
                override fun onResults(results: Bundle?) {
                    if (!gate.accepts(session)) return
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim()
                    if (text.isNullOrEmpty()) {
                        fail(session, UiMessageCodes.voiceNoSpeech)
                    } else {
                        cancel()
                        onRecognized(text)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            armTimeout(session, 60_000L)
            current.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            })
        } catch (_: SecurityException) {
            fail(session, UiMessageCodes.voicePermissionDenied)
        } catch (e: Exception) {
            Log.w("GenesisVoice", "Recognition startup failed: ${e.javaClass.simpleName}")
            fail(session, UiMessageCodes.voiceRecognitionFailed)
        }
    }

    fun cancel() {
        // Invalidate before calling the platform: cancel/destroy can trigger callbacks.
        gate.close()
        watchdog?.let { handler.removeCallbacks(it) }
        watchdog = null
        val old = recognizer
        recognizer = null
        runCatching { old?.cancel() }
        runCatching { old?.destroy() }
    }

    private fun armTimeout(session: Long, delayMillis: Long) {
        watchdog?.let { handler.removeCallbacks(it) }
        watchdog = Runnable {
            if (gate.accepts(session)) {
                Log.w("GenesisVoice", "Recognition watchdog expired")
                fail(session, UiMessageCodes.voiceTimedOut)
            }
        }.also { handler.postDelayed(it, delayMillis) }
    }

    private fun fail(session: Long, code: Int) {
        if (!gate.accepts(session)) return
        cancel()
        onFailure(code)
    }
}

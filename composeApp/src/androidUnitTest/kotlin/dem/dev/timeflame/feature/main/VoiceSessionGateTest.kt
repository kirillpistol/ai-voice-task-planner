package dem.dev.timeflame.feature.main

import android.speech.SpeechRecognizer
import dem.dev.timeflame.util.state.UiMessageCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSessionGateTest {
    @Test fun cancelledCallbacksCannotReachNewRecording() {
        val gate = VoiceSessionGate()
        val old = gate.open()
        gate.close()
        val current = gate.open()
        assertFalse(gate.accepts(old))
        assertTrue(gate.accepts(current))
    }

    @Test fun completedSessionRejectsDuplicateResultsAndErrors() {
        val gate = VoiceSessionGate()
        val session = gate.open()
        assertTrue(gate.accepts(session))
        gate.close()
        assertFalse(gate.accepts(session))
        assertFalse(gate.accepts(gate.open() - 1))
    }

    @Test fun mapsRecoverableFailuresToUsefulMessages() {
        assertEquals(UiMessageCodes.voiceNetworkFailed, voiceErrorMessage(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
        assertEquals(UiMessageCodes.voiceNoSpeech, voiceErrorMessage(SpeechRecognizer.ERROR_NO_MATCH))
        assertEquals(UiMessageCodes.voiceBusy, voiceErrorMessage(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(UiMessageCodes.voicePermissionDenied, voiceErrorMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(UiMessageCodes.voiceLanguageUnavailable, voiceErrorMessage(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
        assertEquals(UiMessageCodes.voiceAudioFailed, voiceErrorMessage(SpeechRecognizer.ERROR_AUDIO))
        assertEquals(UiMessageCodes.voiceRecognitionFailed, voiceErrorMessage(999))
    }
}

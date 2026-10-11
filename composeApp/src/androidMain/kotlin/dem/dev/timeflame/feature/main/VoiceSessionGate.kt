package dem.dev.timeflame.feature.main

/** A cancelled or completed session can never deliver into a later recording. */
internal class VoiceSessionGate {
    private var generation = 0L
    private var active = false

    fun open(): Long {
        generation++
        active = true
        return generation
    }

    fun accepts(session: Long): Boolean = active && session == generation

    fun close() {
        active = false
        generation++
    }
}

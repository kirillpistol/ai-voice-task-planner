package dem.dev.timeflame.feature.main.state

/**
 * Main areas of PISTOL GENESIS. They share a single authenticated screen so
 * that the calendar selection and voice-input state survive switching tabs.
 */
enum class MainSection {
    CALENDAR,
    TASKS,
    SERVICES,
    GENESIS
}

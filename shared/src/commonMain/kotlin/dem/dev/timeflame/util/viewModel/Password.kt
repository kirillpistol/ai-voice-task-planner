package dem.dev.timeflame.util.viewModel

fun String.containsSpecialCharacters(): Boolean {
    val regex = Regex("[^a-zA-Z0-9]")
    return regex.containsMatchIn(this)
}
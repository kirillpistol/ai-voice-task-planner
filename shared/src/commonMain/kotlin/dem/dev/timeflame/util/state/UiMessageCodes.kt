package dem.dev.timeflame.util.state

object UiMessageCodes {
    const val loggingIn = 1
    const val gotErrorLoggingInCheckYourCred = 2

    const val creatingYourAccount = 3
    const val gotErrorWhileCreatingAccount = 4
    const val addAtLeast6Symbols = 5
    const val passwordNeedsToContainBothSmallAndLarge = 6
    const val addSpecialLetters = 7
    const val allGood = 8
    const val startTypingYourPassword = 9

    const val sendingNewPassword = 10
    const val passwordWasSentToYourEmail = 11
    const val thereIsNoUserWithThisEmail = 5

    const val fieldsCannotBeEmpty = 12
    const val passwordStrengthLevel3Needed = 13

    const val loadingTasks = 14
    const val gotErrorWhenGettingLocalUserId = 15
    const val gotErrorLoadingTasks = 16
    const val updatingTask = 17

    const val errorGettingNextMonth = 18
    const val errorGettingPreviousMonth = 19
    const val failedToGetCurrentMonth = 20

    const val taskCreatedSuccessfully = 21
    const val gotAnErrorWhileCreatingTask = 22
}
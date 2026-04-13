package dem.dev.timeflame.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dem.dev.timeflame.R
import dem.dev.timeflame.util.state.UiMessageCodes
import timeflame.composeapp.generated.resources.Res

@Composable
fun getMessage(messageCode: Int?): String {
    return when(messageCode) {
        UiMessageCodes.loggingIn -> stringResource(R.string.logging_in)
        UiMessageCodes.gotErrorLoggingInCheckYourCred -> stringResource(R.string.error_logging_in_check_your_cred)
        UiMessageCodes.creatingYourAccount -> stringResource(R.string.creating_your_account)
        UiMessageCodes.gotErrorWhileCreatingAccount -> stringResource(R.string.got_error_while_creating_account)
        UiMessageCodes.addAtLeast6Symbols -> stringResource(R.string.add_at_least_six_symbols)
        UiMessageCodes.passwordNeedsToContainBothSmallAndLarge -> stringResource(R.string.password_needs_to_contain_both_small_and_large)
        UiMessageCodes.addSpecialLetters -> stringResource(R.string.add_special_letters)
        UiMessageCodes.allGood -> stringResource(R.string.all_good)
        UiMessageCodes.startTypingYourPassword -> stringResource(R.string.start_typing_your_password)
        UiMessageCodes.sendingNewPassword -> stringResource(R.string.sending_new_password)
        UiMessageCodes.passwordWasSentToYourEmail -> stringResource(R.string.password_was_sent)
        UiMessageCodes.thereIsNoUserWithThisEmail -> stringResource(R.string.there_is_no_user)
        UiMessageCodes.fieldsCannotBeEmpty -> stringResource(R.string.fields_cannot_be_empty)
        UiMessageCodes.passwordStrengthLevel3Needed -> stringResource(R.string.password_strength_level_3_needed)
        else -> ""
    }
}
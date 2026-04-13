package dem.dev.timeflame.feature.resetPassword.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dem.dev.timeflame.R
import dem.dev.timeflame.feature.login.presentation.EmailFieldState
import dem.dev.timeflame.feature.resetPassword.ResetPasswordScreenEvent
import dem.dev.timeflame.feature.resetPassword.ResetPasswordScreenState
import dem.dev.timeflame.feature.resetPassword.ResetPasswordScreenViewModel
import dem.dev.timeflame.navigation.Screen
import dem.dev.timeflame.util.*
import dem.dev.timeflame.util.components.LoadingDialog
import dem.dev.timeflame.util.components.MessageDialog
import dem.dev.timeflame.util.components.MessageType
import dem.dev.timeflame.util.components.Screen
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.theme.AppTheme
import dem.dev.timeflame.util.theme.DarkModeHelper

@Composable
fun ResetPasswordScreen(
    modifier: Modifier = Modifier,
    navigateTo: (Screen) -> Unit
) {
    val viewModel = androidKoinViewModel<ResetPasswordScreenViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val isDarkModeEnabled = DarkModeHelper.isDarkModeEnabled()
    val isSystemInDarkMode = isSystemInDarkTheme()
    AppTheme(darkTheme = isDarkModeEnabled ?: isSystemInDarkMode) {
        ResetPasswordScreenView(
            modifier = modifier,
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
private fun ResetPasswordScreenView(
    modifier: Modifier = Modifier,
    state: ResetPasswordScreenState = ResetPasswordScreenState(),
    onEvent: (ResetPasswordScreenEvent) -> Unit = {}
) {
    if (state.screenState is ScreenState.Loading) {
        LoadingDialog(
            isLoading = true,
            message = getMessage(state.screenState.messageCode)
        )
    }
    if (state.screenState is ScreenState.Result) {
        MessageDialog(
            dialog = MessageDialog(
                type = if (state.screenState.resultType == ResultType.SUCCESS) MessageType.SUCCESS else MessageType.ERROR,
                message = getMessage(state.screenState.messageCode)
            ),
        ) { onEvent(ResetPasswordScreenEvent.MessageDialogDismissed) }
    }

    Screen(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ResetPasswordHeader(
            modifier = Modifier.padding(top = 20.dp)
        )

        FieldsSection(
            emailState = state.emailState,
            onEmailChanged = { newEmail ->
                onEvent(ResetPasswordScreenEvent.EmailChanged(newEmail))
            }
        )

        ResetPasswordFooter(
            modifier = Modifier.padding(bottom = 30.dp),
            onSendBtnClicked = { onEvent(ResetPasswordScreenEvent.SendNewPasswordBtnClicked) }
        )
    }
}

@Composable
private fun ResetPasswordHeader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(15.dp))
            .padding(15.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            disabledElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.reset_password),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FieldsSection(
    modifier: Modifier = Modifier,
    emailState: EmailFieldState = EmailFieldState(),
    onEmailChanged: (String) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(0.9f)
    ) {
        Text(
            text = stringResource(R.string.set_your_email),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary
        )
        Text(
            text = stringResource(R.string.will_send_new_password),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            value = emailState.value,
            onValueChange = { onEmailChanged(it) },
            placeholder = {
                Text(text = stringResource(R.string.email), color = MaterialTheme.colorScheme.onSurface)
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                textColor = MaterialTheme.colorScheme.onTertiary
            )
        )
    }
}

@Composable
private fun ResetPasswordFooter(
    modifier: Modifier = Modifier,
    onSendBtnClicked: () -> Unit = {}
) {
    Button(
        onClick = { onSendBtnClicked() },
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            disabledBackgroundColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth(0.8f),
        contentPadding = PaddingValues(vertical = 17.dp)
    ) {
        Text(
            text = stringResource(R.string.send),
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun ResetPasswordScreenPreview() {
    AppTheme {
        ResetPasswordScreenView()
    }
}
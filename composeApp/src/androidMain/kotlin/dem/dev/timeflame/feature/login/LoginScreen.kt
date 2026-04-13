package dem.dev.timeflame.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dem.dev.timeflame.feature.login.presentation.LoginScreenEvent
import dem.dev.timeflame.feature.login.presentation.LoginScreenNavigateOption
import dem.dev.timeflame.feature.login.presentation.LoginScreenState
import dem.dev.timeflame.feature.login.presentation.LoginScreenViewModel
import dem.dev.timeflame.navigation.Screen
import dem.dev.timeflame.util.components.Screen
import dem.dev.timeflame.util.koinViewModel
import dem.dev.timeflame.R
import dem.dev.timeflame.util.theme.AppTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import dem.dev.timeflame.util.components.LoadingDialog
import dem.dev.timeflame.util.components.MessageDialog
import dem.dev.timeflame.util.components.MessageType
import dem.dev.timeflame.util.getMessage
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.theme.DarkModeHelper

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navigateTo: (Screen) -> Unit,
    clearBackStack: () -> Unit
) {
    val viewModel = koinViewModel<LoginScreenViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            when(it) {
                LoginScreenNavigateOption.MainScreen -> {
                    clearBackStack()
                    navigateTo(Screen.Main)
                }
                LoginScreenNavigateOption.RegisterScreen -> navigateTo(Screen.Register)
                LoginScreenNavigateOption.ResetPasswordScreen -> navigateTo(Screen.ResetPassword)
            }
            viewModel.clearState()
        }
    }

    val isDarkModeEnabled = DarkModeHelper.isDarkModeEnabled()
    val isSystemInDarkMode = isSystemInDarkTheme()
    AppTheme(darkTheme = isDarkModeEnabled ?: isSystemInDarkMode) {
        LoginScreenView(
            modifier = modifier,
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
private fun LoginScreenView(
    modifier: Modifier = Modifier,
    state: LoginScreenState = LoginScreenState(),
    onEvent: (LoginScreenEvent) -> Unit = {},
) {
    (state.screenState as? ScreenState.Loading)?.let { screenState ->
        LoadingDialog(
            isLoading = true,
            message = getMessage(screenState.messageCode)
        )
    }
    (state.screenState as? ScreenState.Result)?.let { result ->
        val messageType = when (result.resultType) {
            ResultType.SUCCESS -> MessageType.SUCCESS
            ResultType.FAILURE -> MessageType.ERROR
        }
        MessageDialog(
            dialog = MessageDialog(type = messageType, message = getMessage(result.messageCode)),
        ) { onEvent(LoginScreenEvent.MessageDialogDismissed) }
    }

    Screen(
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LoginScreenHeader(modifier = Modifier.padding(top = 30.dp))

        FieldsSection(
            modifier = Modifier,
            state = state,
            onEvent = onEvent
        )

        LoginScreenFooter(
            modifier = Modifier,
            onEvent = onEvent
        )
    }
}

@Composable
private fun LoginScreenHeader(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(15.dp))
            .padding(15.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = stringResource(R.string.welcome_to),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Image(
                    painter = painterResource(R.drawable.down_arrow),
                    contentDescription = null,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier.padding(top = 15.dp, start = 5.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Image(
                    painter = painterResource(R.drawable.down_arrow),
                    contentDescription = null,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier.padding(top = 28.dp, start = 5.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.log_into_account),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun FieldsSection(
    modifier: Modifier = Modifier,
    state: LoginScreenState = LoginScreenState(),
    onEvent: (LoginScreenEvent) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.email.value,
            onValueChange = { onEvent(LoginScreenEvent.EmailChanged(it)) },
            placeholder = { Text(text = stringResource(R.string.email), color = MaterialTheme.colorScheme.onSurface) },
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
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
            value = state.password.value,
            onValueChange = { onEvent(LoginScreenEvent.PasswordChanged(it)) },
            placeholder = { Text(text = stringResource(R.string.password), color = MaterialTheme.colorScheme.onSurface) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                textColor = MaterialTheme.colorScheme.onTertiary
            ),
            visualTransformation = if (state.password.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val iconSource = if (state.password.showPassword) R.drawable.eye_outlined else R.drawable.eye_off_outline
                Icon(
                    painter = painterResource(iconSource),
                    contentDescription = null,
                    modifier = Modifier.clickable { onEvent(LoginScreenEvent.TogglePasswordVisibilityBtnClicked) },
                    tint = MaterialTheme.colorScheme.onTertiary
                )
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.forgot_password_q),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onEvent(LoginScreenEvent.ResetPasswordClicked) }
            )
        }
    }
}

@Composable
private fun LoginScreenFooter(
    modifier: Modifier = Modifier,
    onEvent: (LoginScreenEvent) -> Unit = {}
) {
    val noAccountText = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onTertiary)) {
            append(stringResource(R.string.no_account_q))
        }
        append(" ")
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(stringResource(R.string.create))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onEvent(LoginScreenEvent.LoginBtnClicked) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colorScheme.primary,
                disabledBackgroundColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f),
            contentPadding = PaddingValues(vertical = 17.dp)
        ) {
            Text(
                text = stringResource(R.string.login),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Divider(modifier = Modifier.fillMaxWidth(0.6f).padding(top = 15.dp))
        Text(
            text = noAccountText,
            modifier = Modifier
                .clickable { onEvent(LoginScreenEvent.CreateAccountClicked) }
                .padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    AppTheme {
        LoginScreenView()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenHeaderPreview() {
    AppTheme {
        LoginScreenHeader(
            modifier = Modifier
        )
    }
}
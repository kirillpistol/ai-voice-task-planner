package dem.dev.timeflame.feature.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dem.dev.timeflame.R
import dem.dev.timeflame.feature.register.navigation.RegisterScreenNavigationOptions
import dem.dev.timeflame.feature.register.presentation.RegisterScreenEvent
import dem.dev.timeflame.feature.register.presentation.RegisterScreenState
import dem.dev.timeflame.feature.register.presentation.RegisterScreenViewModel
import dem.dev.timeflame.navigation.Screen
import dem.dev.timeflame.util.components.LoadingDialog
import dem.dev.timeflame.util.components.MessageDialog
import dem.dev.timeflame.util.components.MessageType
import dem.dev.timeflame.util.components.Screen
import dem.dev.timeflame.util.components.PasswordStrengthIndicator
import dem.dev.timeflame.util.getMessage
import dem.dev.timeflame.util.koinViewModel
import dem.dev.timeflame.util.state.ResultType
import dem.dev.timeflame.util.state.ScreenState
import dem.dev.timeflame.util.theme.AppTheme
import dem.dev.timeflame.util.theme.DarkModeHelper

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    navigateTo: (Screen) -> Unit
) {
    val viewModel = koinViewModel<RegisterScreenViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let {
            when(it) {
                RegisterScreenNavigationOptions.MainScreen -> navigateTo(Screen.Main)
                RegisterScreenNavigationOptions.LoginScreen -> navigateTo(Screen.Login)
            }
        }
    }
    val isDarkModeEnabled = DarkModeHelper.isDarkModeEnabled()
    val isSystemInDarkMode = isSystemInDarkTheme()
    AppTheme(darkTheme = isDarkModeEnabled ?: isSystemInDarkMode) {
        RegisterScreenView(
            modifier = modifier,
            state = state,
            onEvent = viewModel::onEvent
        )
    }
}

@Composable
fun RegisterScreenView(
    modifier: Modifier = Modifier,
    state: RegisterScreenState = RegisterScreenState(),
    onEvent: (RegisterScreenEvent) -> Unit = {}
) {
    if (state.screenState is ScreenState.Loading) {
        LoadingDialog(
            isLoading = true,
            message = getMessage((state.screenState as? ScreenState.Loading)?.messageCode)
        )
    }
    if (state.screenState is ScreenState.Result && (state.screenState as ScreenState.Result).resultType == ResultType.FAILURE) {
        MessageDialog(
            dialog = MessageDialog(type = MessageType.ERROR, message = getMessage((state.screenState as ScreenState.Result).messageCode)),
        ) { onEvent(RegisterScreenEvent.MessageDialogDismissed) }
    }
    Screen(
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        RegisterHeader(modifier = Modifier.padding(top = 30.dp))

        FieldsSection(
            modifier = modifier,
            state = state,
            onEvent = onEvent
        )

        RegisterScreenFooter(
            modifier = Modifier,
            onEvent = onEvent
        )
    }
}

@Composable
private fun RegisterHeader(
    modifier: Modifier = Modifier
) {
    val appIconResId = if (isSystemInDarkTheme()) R.drawable.timeflame_icon_dark else R.drawable.timeflame_icon_light

    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(15.dp))
            .padding(vertical = 15.dp, horizontal = 25.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(appIconResId),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            Column(
                modifier = Modifier.padding(start = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${stringResource(R.string.create)}\n${stringResource(R.string.account)}",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSecondary
                )

                Text(
                    text = stringResource(R.string.in_timeflame),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary
                )

                Text(
                    text = stringResource(R.string.takes_30_sec),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }
}

@Composable
private fun FieldsSection(
    modifier: Modifier = Modifier,
    state: RegisterScreenState = RegisterScreenState(),
    onEvent: (RegisterScreenEvent) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.8f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.usernameState.value,
            onValueChange = { onEvent(RegisterScreenEvent.NameChanged(it)) },
            placeholder = { Text(text = stringResource(R.string.name), color = MaterialTheme.colorScheme.onSurface) },
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
            value = state.emailState.value,
            onValueChange = { onEvent(RegisterScreenEvent.EmailChanged(it)) },
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
            value = state.passwordState.value,
            onValueChange = { onEvent(RegisterScreenEvent.PasswordChanged(it)) },
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
            visualTransformation = if (state.passwordState.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val iconSource = if (state.passwordState.showPassword) R.drawable.eye_outlined else R.drawable.eye_off_outline
                Image(
                    painter = painterResource(iconSource),
                    contentDescription = null,
                    modifier = Modifier.clickable { onEvent(RegisterScreenEvent.TogglePasswordVisibilityBtnClicked) }
                )
            }
        )
        PasswordStrengthIndicator(
            strengthLevel = state.passwordState.passwordStrength,
            currentAction = getMessage(state.passwordState.passwordStrengthActionCode),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.forgot_password_q),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RegisterScreenFooter(
    modifier: Modifier = Modifier,
    onEvent: (RegisterScreenEvent) -> Unit = {}
) {
    val alreadyHaveAnAccount = buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onTertiary)) {
            append(stringResource(R.string.already_have_an_account_q))
        }
        append(" ")
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(stringResource(R.string.login))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onEvent(RegisterScreenEvent.RegisterBtnClicked) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f),
            contentPadding = PaddingValues(vertical = 17.dp)
        ) {
            Text(
                text = stringResource(R.string.register),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.6f).padding(top = 15.dp))

        Text(
            text = alreadyHaveAnAccount,
            modifier = Modifier
                .clickable { onEvent(RegisterScreenEvent.ToLoginClicked) }
                .padding(top = 10.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
fun RegisterHeaderPreview() {
    AppTheme {
        RegisterHeader()
    }
}

@Preview
@Composable
fun RegisterScreenPreview() {
    AppTheme {
        RegisterScreenView()
    }
}
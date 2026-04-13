//
//  LoginScreenViewModel.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared

class LoginScreenViewModel: ObservableObject {
    
    var onNavigate: (Router.Destination) -> Void
    @Published var state = LoginScreenState()
    private let loginUseCase = UseCaseComponent().loginUseCase
    private let localAuthManager = PreferencesComponent().localAuthManager
    
    init(onNavigate: @escaping (Router.Destination) -> Void) {
        self.onNavigate = onNavigate
    }
    
    func onEvent(event: LoginScreenEvent) {
        switch event {
        case .CreateAccountClicked: onCreateAccountClicked()
        case .ResetPasswordClicked: onResetPasswordClicked()
        case .TogglePasswordViisbilityBtnClicked: onTogglePasswordViisbilityBtnClicked()
        case .LoginBtnClicked: onLoginBtnClicked()
        case .MessageDialogDismissed: onMessageDialogDismissed()
        }
    }
    
    private func onCreateAccountClicked() {
        onNavigate(Router.Destination.register)
    }
    private func onResetPasswordClicked() {
        onNavigate(Router.Destination.resetPassword)
    }
    private func onTogglePasswordViisbilityBtnClicked() {
        state = state.copy(passwordState: state.passwordState.copy(showPassword: !state.passwordState.showPassword))
    }
    private func onLoginBtnClicked() {
        login(email: state.emailState.text, password: state.passwordState.text)
    }
    private func onMessageDialogDismissed() {
        state = state.copy(screenState: ScreenState.idle)
    }
    
    private func login(email: String, password: String) {
        state = state.copy(screenState: ScreenState.loading(messageCode: MessageCodes.loggingIn))
        _Concurrency.Task {
            do {
                let result = try await loginUseCase.invoke(email: email, password: password)
                if result.isSuccess() {
                    onNavigate(Router.Destination.main)
                } else {
                    state = state.copy(screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.gotErrorLoggingInCheckYourCred))
                }
            } catch {
                state = state.copy(screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.gotUnexpectedErrorLoggingIn))
            }
        }
    }
    
    func checkAuth() -> Bool {
        if self.localAuthManager.getCurrentUser() != nil {
            return true
        }
        return false
    }
}

//
//  RegisterScreenViewModel.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 08/11/2024.
//  Copyright © 2024 orgName. All rights reserved.
//
import SwiftUI
import Shared

class RegisterScreenViewModel: ObservableObject {
    
    var onNavigate: (Router.Destination) -> Void
    @Published var state = RegisterScreenState()
    private let registerUseCase = UseCaseComponent().registerUseCase
    
    init(onNavigate: @escaping (Router.Destination) -> Void) {
        self.onNavigate = onNavigate
    }
    
    func onEvent(event: RegisterScreenEvent) {
        switch event {
        case .MessageDialogDismissed: onMessageDialogDismissed()
        case .RegisterBtnClicked: onRegisterBtnClicked()
        case .ToLoginClicked: onToLoginClicked()
        }
    }
    
    private func onMessageDialogDismissed() {
        state = state.copy(screenState: ScreenState.idle)
    }
    private func onRegisterBtnClicked() {
        if state.usernameState.text.isEmpty || state.emailState.text.isEmpty || state.passwordState.text.isEmpty {
            state = state.copy(screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.fieldsCannotBeEmpty))
            return
        }
        register(username: state.usernameState.text, email: state.emailState.text, password: state.passwordState.text)
    }
    private func onToLoginClicked() {
        onNavigate(.login)
    }
    
    private func register(username: String, email: String, password: String) {
        if (state.emailState.text.isEmpty || state.usernameState.text.isEmpty || state.passwordState.text.isEmpty) {
            state = state.copy(screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.fieldsCannotBeEmpty))
            return
        }
        state = state.copy(screenState: ScreenState.loading(messageCode: MessageCodes.registering))
        _Concurrency.Task {
            do {
                let result = try await registerUseCase.invoke(name: username, email: email, password: password)
                if result.isSuccess() {
                    onNavigate(Router.Destination.main)
                } else {
                    if result.code == 470 {
                        state = state.copy(
                            screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.gotErrorUserAlreadyExists)
                        )
                    } else {
                        state = state.copy(
                            screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.gotUnexpectedErrorSigningUp)
                        )
                    }
                }
            } catch {
                state = state.copy(screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.gotUnexpectedErrorSigningUp))
            }
        }
    }
}

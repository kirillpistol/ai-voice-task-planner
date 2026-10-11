//
//  ResetPasswordScreenViewModel.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 13/11/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared

class ResetPasswordScreenViewModel: ObservableObject {
    var onNavigate: (Router.Destination) -> Void
    @Published var state = ResetPasswordScreenState()
    private let resetPasswordUseCase = ResetPasswordUseCaseWrapper().useCase
    
    init(onNavigate: @escaping (Router.Destination) -> Void) {
        self.onNavigate = onNavigate
    }
    
    func onEvent(event: ResetPasswordScreenEvent) {
        switch event {
        case .SendBtnClicked: onSendBtnClicked()
        case .MessageDialogDismissed: onMessageDialogDismissed()
        }
    }
    
    private func onSendBtnClicked() {
        if state.emailState.text.isEmpty {
            state = state.copy(screenState: ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.fieldsCannotBeEmpty))
            return
        }
        state = state.copy(screenState: ScreenState.loading(messageCode: MessageCodes.sendingNewPassword))
        resetPassword(email: state.emailState.text)
    }
    private func onMessageDialogDismissed() {
        state = state.copy(screenState: ScreenState.idle)
    }
    
    private func resetPassword(email: String) {
        _Concurrency.Task {
            do {
                let res = try await resetPasswordUseCase.invoke(email: email)
                if res.isSuccess() {
                    await MainActor.run {
                        state = state.copy(screenState: ScreenState.result(
                            resultType: ResultType.success,
                            messageCode: MessageCodes.sentNewPassword
                        ))
                    }
                } else {
                    await MainActor.run {
                        state = state.copy(screenState: ScreenState.result(
                            resultType: ResultType.failure,
                            messageCode: MessageCodes.userWithEmailDoesntExist
                        ))
                    }
                }
            } catch {
                state = state.copy(screenState: ScreenState.result(
                    resultType: ResultType.failure,
                    messageCode: MessageCodes.gotUnexpectedErrorSendingNewPassword
                ))
            }
            
        }
    }
}

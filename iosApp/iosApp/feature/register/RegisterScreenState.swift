//
//  LoginScreenState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 08/11/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct RegisterScreenState {
    var screenState: ScreenState = ScreenState.idle
    var usernameState: DefaultFieldState = DefaultFieldState()
    var emailState: DefaultFieldState = DefaultFieldState()
    var passwordState: PasswordFieldState = PasswordFieldState()
    
    func copy(
        screenState: ScreenState? = nil,
        usernameState: DefaultFieldState? = nil,
        emailState: DefaultFieldState? = nil,
        passwordState: PasswordFieldState? = nil
    ) -> Self {
        return RegisterScreenState(
            screenState: screenState ?? self.screenState,
            usernameState: usernameState ?? self.usernameState,
            emailState: emailState ?? self.emailState,
            passwordState: passwordState ?? self.passwordState
        )
    }
}

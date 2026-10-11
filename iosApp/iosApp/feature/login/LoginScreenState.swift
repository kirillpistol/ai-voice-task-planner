//
//  LoginScreenState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct LoginScreenState {
    var screenState: ScreenState = ScreenState.idle
    var emailState: DefaultFieldState = DefaultFieldState()
    var passwordState: PasswordFieldState = PasswordFieldState()
    var navigateTo: Router.Destination? = nil
    
    func copy(
        screenState: ScreenState? = nil,
        emailState: DefaultFieldState? = nil,
        passwordState: PasswordFieldState? = nil,
        navigateTo: Router.Destination? = nil
    ) -> Self {
        return LoginScreenState(
            screenState: screenState ?? self.screenState,
            emailState: emailState ?? self.emailState,
            passwordState: passwordState ?? self.passwordState,
            navigateTo: navigateTo ?? self.navigateTo
        )
    }
}

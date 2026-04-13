//
//  ResetScreenState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 13/11/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct ResetPasswordScreenState {
    var screenState: ScreenState = ScreenState.idle
    var emailState: DefaultFieldState = DefaultFieldState()
    
    func copy(
        screenState: ScreenState? = nil,
        emailState: DefaultFieldState? = nil
    ) -> Self {
        return ResetPasswordScreenState(
            screenState: screenState ?? self.screenState,
            emailState: emailState ?? self.emailState
        )
    }
}

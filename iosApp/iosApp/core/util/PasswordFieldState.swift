//
//  PasswordFieldState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct PasswordFieldState {
    var text: String = ""
    var showPassword: Bool = false
    var error: String? = nil
    
    func copy(
        text: String? = nil,
        showPassword: Bool? = nil,
        error: String? = nil
    ) -> PasswordFieldState {
        return PasswordFieldState(
            text: text ?? self.text,
            showPassword: showPassword ?? self.showPassword,
            error: error ?? self.error
        )
    }
}

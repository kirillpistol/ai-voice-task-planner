//
//  EmailFieldState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct DefaultFieldState {
    var text: String = ""
    var error: String? = nil
    
    func copy(
        text: String? = nil,
        error: String? = nil
    ) -> DefaultFieldState {
        return DefaultFieldState(
            text: text ?? self.text,
            error: error ?? self.error
        )
    }
}

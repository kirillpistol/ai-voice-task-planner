//
//  ScreenState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

enum ScreenState {
    case idle
    case loading(messageCode: Int)
    case result(resultType: ResultType, messageCode: Int)
}

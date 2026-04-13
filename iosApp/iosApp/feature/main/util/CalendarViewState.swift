//
//  CalendarViewState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 15/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

enum CalendarViewState {
    case Week
    case Month
}

extension CalendarViewState {
    func toggle() -> CalendarViewState {
        return switch self {
        case CalendarViewState.Month:
            CalendarViewState.Week
        case CalendarViewState.Week:
            CalendarViewState.Month
        }
    }
}

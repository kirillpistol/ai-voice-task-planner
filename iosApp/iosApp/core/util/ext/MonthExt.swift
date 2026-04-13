//
//  MonthExt.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 19/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared

extension Month {
    func getName() -> String {
        return switch self.number {
        case 1: NSLocalizedString("january", comment: "")
        case 2: NSLocalizedString("february", comment: "")
        case 3: NSLocalizedString("march", comment: "")
        case 4: NSLocalizedString("april", comment: "")
        case 5: NSLocalizedString("may", comment: "")
        case 6: NSLocalizedString("june", comment: "")
        case 7: NSLocalizedString("july", comment: "")
        case 8: NSLocalizedString("august", comment: "")
        case 9: NSLocalizedString("september", comment: "")
        case 10: NSLocalizedString("october", comment: "")
        case 11: NSLocalizedString("november", comment: "")
        case 12: NSLocalizedString("december", comment: "")
        default: NSLocalizedString("error_getting_current_month", comment: "")
        }
    }
}

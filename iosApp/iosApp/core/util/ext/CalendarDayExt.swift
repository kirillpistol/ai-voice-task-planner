//
//  CalendarDayExt.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 12/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared

extension Array where Iterator.Element == CalendarDay {
    func sorted() -> [CalendarDay] {
        return self.sorted(by: { (day1: CalendarDay, day2: CalendarDay) -> Bool in
            return day1.day.timestamp() < day2.day.timestamp()
        })
    }
}

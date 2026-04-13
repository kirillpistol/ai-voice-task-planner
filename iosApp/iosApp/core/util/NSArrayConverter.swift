//
//  NSArrayConverter.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 16/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared

extension NSArray {
    func toArray() -> [Task] {
        return ((self as NSArray) as? [Task] ?? [])
    }
    func toIdentifiableArray() -> [IdentifiableTask] {
        return ((self as NSArray) as? [Task] ?? []).map { task in
            return IdentifiableTask(id: Int(task.id), task: task)
        }
    }
    func toCalendarDayArray() -> [CalendarDay] {
        return ((self as NSArray) as? [CalendarDay] ?? [])
    }
}

extension Array {
    func toNSMutableArray() -> NSMutableArray {
        let arr = NSMutableArray()
        arr.addObjects(from: self)
        
        return arr
    }
}

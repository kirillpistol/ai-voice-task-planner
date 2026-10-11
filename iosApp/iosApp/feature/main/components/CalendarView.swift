//
//  CalendarView.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 02/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared

struct CalendarView: View {
    
    @Binding var currentMonthDays: [CalendarDay]
    @Binding var selectedDayIndex: Int
    
    private let daysShortNames = [
        1: NSLocalizedString("monday", comment: ""),
        2: NSLocalizedString("tuesday", comment: ""),
        3: NSLocalizedString("wednesday", comment: ""),
        4: NSLocalizedString("thursday", comment: ""),
        5: NSLocalizedString("friday", comment: ""),
        6: NSLocalizedString("saturday", comment: ""),
        7: NSLocalizedString("sunday", comment: "")
    ]
    
    var body: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top) {
                ForEach(Array(daysShortNames.keys.sorted()), id: \.self) { key in
                    VStack {
                        CalendarItem(text: daysShortNames[key] ?? "1", textColor: Color("Stroke"))
                        
                        let days = currentMonthDays.filter({ $0.day.dayOfWeek == key })
                        
                        ForEach((0..<days.count), id: \.self) { index in
                            let day = days[index]
                            
                            if index == 0 && day.day.after(date: currentMonthDays[0].day) &&
                                day.day.dayOfWeek < currentMonthDays[0].day.dayOfWeek {
                                EmptyCalendarCell()
                            }
                            CalendarItem(
                                text: String(day.day.dayOfMonth),
                                isSelected: (currentMonthDays[selectedDayIndex] == day),
                                tasksAmount: day.tasks.count,
                                onSelected: {
                                    if let originalIndex = currentMonthDays.firstIndex(of: day) {
                                        selectedDayIndex = originalIndex
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

#Preview {
    let date = KDateTime.companion.now()
    let currentMonthDays = [
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 1), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 2), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 3), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 4), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 5), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 6), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 7), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 8), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 9), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 10), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 11), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 12), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 13), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 14), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 15), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 16), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 17), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 18), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 19), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 20), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 21), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 22), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 23), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 24), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 25), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 26), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 27), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 28), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 29), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 30), tasks: []),
        CalendarDay(day: KDateTime(year: 2024, month: 12, day: 31), tasks: []),
    ]
    CalendarView(
        currentMonthDays: .constant(currentMonthDays),
        selectedDayIndex: .constant(1)
    )
}

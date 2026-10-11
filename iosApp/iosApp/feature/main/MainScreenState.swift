//
//  MainScreenState.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 15/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared

struct MainScreenState {
    var screenState: ScreenState = ScreenState.idle
    var currentMonth: Month? = Month.Companion().current()
    var selectedDay: CalendarDay = CalendarDay(day: KDateTime.Companion().now(), tasks: [])
    var calendarViewState: CalendarViewState = CalendarViewState.Month
    var navigateTo: Router.Destination? = nil
    var selectedTaskToEdit: Task? = nil
    
    func copy(
        screenState: ScreenState? = nil,
        currentMonth: Month? = nil,
        selectedDay: CalendarDay? = nil,
        calendarViewState: CalendarViewState? = nil,
        navigateTo: Router.Destination? = nil,
        selectedTaskToEdit: Task? = nil
    ) -> Self {
        return MainScreenState(
            screenState: screenState ?? self.screenState,
            currentMonth: currentMonth ?? self.currentMonth,
            selectedDay: selectedDay ?? self.selectedDay,
            calendarViewState: calendarViewState ?? self.calendarViewState,
            navigateTo: navigateTo ?? self.navigateTo,
            selectedTaskToEdit: selectedTaskToEdit ?? self.selectedTaskToEdit
        )
    }
}

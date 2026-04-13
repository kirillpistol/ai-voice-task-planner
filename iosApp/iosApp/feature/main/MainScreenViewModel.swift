//
//  MainScreenViewModel.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 15/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared

class MainScreenViewModel: ObservableObject {
    var onNavigate: (Router.Destination) -> Void

    @Published var screenState: ScreenState = ScreenState.idle
    @Published var currentMonth: Month = Month.Companion().current()!
    @Published var currentMonthDays: [CalendarDay] = []
    @Published var selectedDayIndex: Int = 0
    @Published var calendarViewState: CalendarViewState = CalendarViewState.Month
    @Published var navigateTo: Router.Destination? = nil
    @Published var selectedTaskToEdit: Task? = nil
    @Published var editTaskBottomSheetRequested: Bool = false
    
    private let localAuthManager = PreferencesComponent().localAuthManager
    private let loadCalendarForMonthUseCase = UseCaseComponent().loadCalendarForMonthUseCase
    private let createTaskUseCase = UseCaseComponent().createTaskUseCase
    private let deleteTaskUseCase = UseCaseComponent().deleteTaskUseCase
    private let updateTaskUseCase = UseCaseComponent().updateTaskUseCase
    
    init(onNavigate: @escaping (Router.Destination) -> Void) {
        self.onNavigate = onNavigate
        
        loadTasks(month: currentMonth)
    }
    
    func onEvent(event: MainScreenEvent) {
        switch event {
        case .NextMonthBtnClicked:
            onNextMonthClicked()
            
        case .PreviousMonthBtnClicked:
            onPreviousMonthClicked()
            
        case .CalendarViewSwitch:
            onCalendarViewSwitch()
            
        case .CalendarDayClicked(let dayIndex):
            onCalendarDayClicked(dayIndex: dayIndex)
            
        case .EditTaskClicked(let task):
            onEditTaskClicked(selectedTask: task)
            
        case .DeleteTaskClicked(let task):
            onDeleteTaskClicked(task: task)
            
        case .TaskStatusChangeClicked(let task):
            onTaskStatusChangeClicked(task: task)
            
        case .MessageDialogDismissed:
            onMessageDialogDismissed()
            
        case .EditTaskBottomSheetDismissed:
            onEditTaskBottomSheetDismissed()
            
        case .SaveUpdatedTaskClicked(updatedTask: let updatedTask):
            onSaveUpdatedTaskClicked(updatedTask: updatedTask)
        case .RecordNewTaskBtnClicked:
            onRecordNewTaskBtnClicked()
            
        case .NewTaskRecordingFinished(recognizedRequest: let recognizedRequest):
            <#code#>
        case .NewTaskRecordingDismissed:
            <#code#>
        case .RecognizedTaskTextEdited(updatedText: let updatedText):
            <#code#>
        case .CreateNewTaskClicked:
            <#code#>
        }
    }
    
    func onRecordNewTaskBtnClicked() {
        
    }
    func onMessageDialogDismissed() {
        screenState = ScreenState.idle
    }
    func onNextMonthClicked() {
        if let nextMonth = currentMonth.next() {
            currentMonth = nextMonth
            
            loadTasks(month: nextMonth)
        } else {
            screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorGettingNextMonth)
        }
    }
    func onPreviousMonthClicked() {
        if let previousMonth = currentMonth.previous() {
            currentMonth = previousMonth
            
            loadTasks(month: previousMonth)
        } else {
            screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorGettingPreviousMonth)
        }
    }
    func onCalendarViewSwitch() {
        calendarViewState = calendarViewState.toggle()
    }
    func onCalendarDayClicked(dayIndex: Int) {
        selectedDayIndex = dayIndex
    }
    func onEditTaskBottomSheetDismissed() {
        selectedTaskToEdit = nil
        editTaskBottomSheetRequested = false
    }
    func onEditTaskClicked(selectedTask: Task) {
        selectedTaskToEdit = selectedTask
        editTaskBottomSheetRequested = true
    }
    func onSaveUpdatedTaskClicked(updatedTask: Task) {
        if let selectedTask = selectedTaskToEdit {
            selectedTaskToEdit = nil
            editTaskBottomSheetRequested = false
            
            let currentSelectedDay = currentMonthDays[selectedDayIndex]
            let updatedTaskDateTime = KDateTime.Companion().fromUtcTimestamp(timestamp: updatedTask.timestamp)
            
            // if date of the task was changed to another month or another year
            if updatedTaskDateTime.monthNumber != currentSelectedDay.day.monthNumber
                || updatedTaskDateTime.year != currentSelectedDay.day.year {
                if let month = Month.Companion().byNumberAndYear(monthNumber: updatedTaskDateTime.monthNumber, year: updatedTaskDateTime.year) {
                    updateTaskAndGotoMonth(task: updatedTask, month: month, newTaskDayOfMonth: Int(updatedTaskDateTime.dayOfMonth) - 1)
                    return
                }
            }
            if updatedTaskDateTime.dayOfMonth != currentSelectedDay.day.dayOfMonth {
                /*
                 If task date updated and now it's not on the selected day
                 1. Remove old(selected) task from the selected day
                 2. Add updated task to the new day
                 3. Set updated task new day as a selected day
                 */
                self.currentMonthDays[selectedDayIndex].tasks.remove(selectedTask)
                self.currentMonthDays[Int(updatedTaskDateTime.dayOfMonth) - 1].tasks.add(updatedTask)
                self.selectedDayIndex = Int(updatedTaskDateTime.dayOfMonth) - 1 /* Index of new day of the updated task */
                
                updateTask(task: updatedTask)
                return
            }
            /*
             If task date was not changed need to simply update task in the selected day
             */
            self.currentMonthDays[selectedDayIndex].tasks.remove(selectedTask)
            self.currentMonthDays[selectedDayIndex].tasks.add(updatedTask)
            updateTask(task: updatedTask)
        }
    }
    func onDeleteTaskClicked(task: Task) {
        screenState = ScreenState.loading(messageCode: MessageCodes.deletingTask)
        
        _Concurrency.Task {
            do {
                let response = try await deleteTaskUseCase.invoke(taskId: task.id)
                
                if response.isSuccess() {
                    DispatchQueue.main.async {
                        self.screenState = ScreenState.result(resultType: ResultType.success, messageCode: MessageCodes.taskSuccessfullyDeleted)
                    }
                    
                    // remove task from the selected day
                    self.currentMonthDays[selectedDayIndex].tasks.remove(task)
                }
            } catch {
                screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorDeletingTask)
            }
        }
    }
    func onTaskStatusChangeClicked(task: Task) {
        let currentSelectedDay = currentMonthDays[selectedDayIndex]
        let updatedTask = task.doCopy(id: task.id, text: task.text, userId: task.userId, timestamp: task.timestamp, completed: !task.completed, notificationSent: task.notificationSent)
        
        
        let updatedSelectedDayTasks = currentSelectedDay.tasks.toArray().map({ currTask in
            if currTask.id == task.id {
                return updatedTask
            }
            return currTask
        }).toNSMutableArray()
        
        currentMonthDays[selectedDayIndex] = currentSelectedDay.doCopy(day: currentSelectedDay.day, tasks: updatedSelectedDayTasks)
        
        updateTaskStatus(task: updatedTask)
    }
    
    func updateTask(task: Task) {
        _Concurrency.Task {
            do {
                try await updateTaskUseCase.invoke(task: task)
            } catch {
                screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorUpdatingTask)
            }
        }
    }
    func updateTaskAndGotoMonth(task: Task, month: Month, newTaskDayOfMonth: Int) {
        screenState = ScreenState.loading(messageCode: 0)
        
        _Concurrency.Task {
            do {
                try await updateTaskUseCase.invoke(task: task)
                
                loadTasks(month: month, selectedDayIndex: newTaskDayOfMonth)
            } catch {
                screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorUpdatingTask)
            }
        }
    }
    func updateTaskStatus(task: Task) {
        _Concurrency.Task {
            do {
                try await updateTaskUseCase.invoke(task: task)
            } catch {
                screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorUpdatingTaskStatus)
            }
        }
    }
    func loadTasks(month: Month, selectedDayIndex: Int? = nil) {
        DispatchQueue.main.async {
            self.screenState = ScreenState.loading(messageCode: MessageCodes.loadingTasks)
        }
        
        _Concurrency.Task {
            do {
                if let currentUser = localAuthManager.getCurrentUser() {
                    let result = try await loadCalendarForMonthUseCase.invoke(userId: currentUser.id, month: month)
                    
                    if result.isSuccess() && result.data != nil {
                        
                        DispatchQueue.main.async {
                            self.screenState = ScreenState.idle
                            self.currentMonth = month.sortTasksByDays(tasks: result.data!.toArray())
                            self.currentMonthDays = self.currentMonth.days.toCalendarDayArray()
                            
                            // if we want to select specific day
                            if let selectedDayIndex = selectedDayIndex {
                                self.selectedDayIndex = selectedDayIndex
                            } else {
                                self.initializeDefaultSelectedDay()
                            }
                        }
                    } else {
                        DispatchQueue.main.async {
                            self.screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorLoadingTasks)
                        }
                    }
                } else {
                    DispatchQueue.main.async {
                        self.screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorFailedToGetLocalUserId)
                    }
                }
            } catch {
                DispatchQueue.main.async {
                    self.screenState = ScreenState.result(resultType: ResultType.failure, messageCode: MessageCodes.errorFailedToGetLocalUserId)
                }
            }
        }
    }
    
    // the default selected day is today, need to get his index in the calendar array of all days
    func initializeDefaultSelectedDay() {
        let currentDay = KDateTime.Companion().now()
        if let index = self.currentMonthDays.firstIndex(where: { day in return day.day.dayOfMonth == currentDay.dayOfMonth }) {
            selectedDayIndex = index
        }
    }
}

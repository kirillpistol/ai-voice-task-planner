//
//  MainView.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 28/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared

struct MainScreen: View {
    
    @State var editTaskBottomSheetRequested: Bool = false
    
    @EnvironmentObject var router: Router
    @StateObject var viewModel: MainScreenViewModel =
        MainScreenViewModel(onNavigate: { _ in})
    
    var body: some View {
        VStack {
            VStack {
                headerSection
                
                switch viewModel.screenState {
                case .idle:
                    calendarSection
                        .padding(.top, 20)
                    
                    Spacer()
                case .loading(let messageCode):
                    Spacer()
                    LoadingView(loading: true, message: getMessage(messageCode: messageCode))
                    Spacer()
                case .result(let resultType, let messageCode):
                    Spacer()
                    ResultView(
                        resultType: resultType,
                        messageCode: messageCode,
                        onDismiss: { viewModel.onEvent(event: MainScreenEvent.MessageDialogDismissed) }
                    )
                    Spacer()
                }
            }
            .padding(.leading, 10)
            .padding(.trailing, 10)
            
            List {
                if (!viewModel.currentMonthDays.isEmpty) {
                    ForEach(viewModel.currentMonthDays[viewModel.selectedDayIndex].tasks.toIdentifiableArray()) { identifiableTask in
                        TaskItem(
                            task: identifiableTask.task,
                            onStatusChangeClicked: {
                                viewModel.onEvent(event: MainScreenEvent.TaskStatusChangeClicked(task: identifiableTask.task))
                            },
                            onDeleteTaskClicked: {
                                viewModel.onEvent(event: MainScreenEvent.DeleteTaskClicked(task: identifiableTask.task))
                            },
                            onEditTaskClicked: {
                                viewModel.onEvent(event: MainScreenEvent.EditTaskClicked(task: identifiableTask.task))
                            }
                        )
                    }
                }
            }
        }
        .background(Color("Background"))
        .sheet(isPresented: $viewModel.editTaskBottomSheetRequested, content: {
            if let selectedTask = viewModel.selectedTaskToEdit {
                EditTaskBottomSheet(
                    selectedTask: selectedTask,
                    onDismiss: {
                        viewModel.onEvent(event: MainScreenEvent.EditTaskBottomSheetDismissed)
                    },
                    onTaskSave: { updatedTask in
                        viewModel.onEvent(event: MainScreenEvent.SaveUpdatedTaskClicked(updatedTask: updatedTask))
                    })
            }
        })
    }
    
    var headerSection: some View {
        HStack {
            HStack {
                Text(NSLocalizedString("your", comment: ""))
                    .foregroundColor(Color("TextColor"))
                    .font(.system(size: 24))
                
                Text(NSLocalizedString("tasks", comment: ""))
                    .foregroundColor(Color("Primary"))
                    .font(.system(size: 24))
            }
            Spacer()
            
            Image("TimeFlameIcon")
                .frame(width: 50, height: 50)
                .clipShape(Circle())
                .background {
                    Circle()
                        .stroke(Color("Primary"), lineWidth: 1)
                }
        }
    }
    
    var calendarSection: some View {
        VStack {
            HStack {
                Button {
                    viewModel.onEvent(event: MainScreenEvent.PreviousMonthBtnClicked)
                } label: {
                    Image(systemName: "chevron.backward")
                        .tint(Color("TextColor"))
                        .padding(10)
                        .background(Color("OnBackground"))
                        .cornerRadius(10)
                }
                .shadow(radius: 2)
                
                Spacer()
                
                Text(String(viewModel.currentMonth.getName()))
                
                Spacer()
                
                Button {
                    viewModel.onEvent(event: MainScreenEvent.NextMonthBtnClicked)
                } label: {
                    Image(systemName: "chevron.forward")
                        .tint(Color("TextColor"))
                        .padding(10)
                        .background(Color("OnBackground"))
                        .cornerRadius(10)
                }
                .shadow(radius: 2)
            }
            CalendarView(
                currentMonthDays: $viewModel.currentMonthDays,
                selectedDayIndex: $viewModel.selectedDayIndex
            )
        }
    }
}

#Preview {
    MainScreen()
}

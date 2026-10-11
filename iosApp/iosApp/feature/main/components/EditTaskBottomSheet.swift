//
//  EditTaskBottomSheet.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 08/01/2025.
//  Copyright © 2025 orgName. All rights reserved.
//

import SwiftUI
import Shared

struct EditTaskBottomSheet: View {
    
    var selectedTask: Task
    var onDismiss: () -> Void
    var onTaskSave: (Task) -> Void
    
    @State private var selectedDate: Date
    @State private var taskTitle: String = ""
    @State private var error: String? = nil
    
    init(selectedTask: Task, onDismiss: @escaping () -> Void, onTaskSave: @escaping (Task) -> Void) {
        self.selectedTask = selectedTask
        self.onDismiss = onDismiss
        self.onTaskSave = onTaskSave
        self.taskTitle = selectedTask.text
        
        print("Timestamp 1: \(selectedTask.timestamp)")
        print("Curr timezone seconds from GMT: \(TimeZone_iosKt.localTimeZone.secondsFromGMT())")
        
        let taskTimestamp = KDateTime.Companion().fromUtcTimestamp(timestamp: selectedTask.timestamp).timestamp()
        print("Timestamp 2: \(taskTimestamp)")
        self.selectedDate = Date(timeIntervalSince1970: TimeInterval(taskTimestamp/1000))
    }
    
    var body: some View {
        VStack {
            HStack {
                Text(NSLocalizedString("edit_task", comment: ""))
                    .foregroundColor(Color("TextColor"))
                    .font(.headline)
                Spacer()
                Button(action: {
                    onDismiss()
                }) {
                    Image(systemName: "xmark")
                        .foregroundColor(Color("TextColor"))
                }
            }.padding(.top, 20)
            Spacer()
            DatePicker(
                selection: $selectedDate,
                displayedComponents: [.date, .hourAndMinute]
            ) {
                Text(NSLocalizedString("datetime", comment: ""))
            }.padding(.top, 20)
            
            if let currError = error {
                Text(currError)
                    .foregroundColor(.white)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .topLeading)
                    .background(.red)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .padding(.top, 20)
                    
            }
            TextField(
                NSLocalizedString("task_title", comment: ""),
                text: $taskTitle
            )
            .padding(15)
            .textInputAutocapitalization(.never)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color("Stroke"), lineWidth: 1))
            .padding(.top, 10)
            
            Spacer()
            
            Button {
                if (taskTitle.isEmpty) {
                    error = NSLocalizedString("fields_cannot_be_empty", comment: "")
                    return
                }
                
                error = nil
                
                print("Timestamp 3: \(KDateTime(date: selectedDate).utcTimestamp())")
                let updatedTask = selectedTask.doCopy(
                    id: selectedTask.id,
                    text: taskTitle,
                    userId: selectedTask.userId,
                    timestamp: KDateTime(date: selectedDate).utcTimestamp(),
                    completed: selectedTask.completed,
                    notificationSent: selectedTask.notificationSent)
                
                onTaskSave(updatedTask)
            } label: {
                Text(NSLocalizedString("save", comment: ""))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 100)
            .padding(.vertical, 15)
            .background(Color("Primary"))
            .cornerRadius(10)
            .padding(.top, 25)
        }
        .padding(.leading, 20)
        .padding(.trailing, 20)
    }
}

#Preview {
    EditTaskBottomSheet(
        selectedTask: Task(id: 0, text: "Zoom conference", userId: "", timestamp: 12323532, completed: false, notificationSent: false),
        onDismiss: {},
        onTaskSave: {_ in }
    )
}

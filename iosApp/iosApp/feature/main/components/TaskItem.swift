//
//  TaskItem.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 20/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared

struct TaskItem: View {
    
    let task: Task
    var onStatusChangeClicked: () -> Void = {}
    var onDeleteTaskClicked: () -> Void = {}
    var onEditTaskClicked: () -> Void = {}
    
    var body: some View {
        let taskCompletenessIndicatorColor = ((task.completed) ? Color("Green") : Color("Primary"))
        HStack {
            Rectangle()
                .fill(taskCompletenessIndicatorColor)
                .frame(width: 3)
                .cornerRadius(10)
            
            VStack(alignment: .leading) {
                Text(KDateTime.Companion().fromUtcTimestamp(timestamp: task.timestamp).formatToString(format: "dd.MM.yyyy HH:mm"))
                
                Text(task.text)
                    .foregroundColor(Color("TextColor"))
            }
            Spacer()
        }
        .frame(height: 50)
        .background(Color("OnBackground"))
        .cornerRadius(10)
        .swipeActions(edge: .leading) {
            Button {
                onStatusChangeClicked()
            } label: {
                Image(systemName: "checkmark")
            }
            Button {
                onDeleteTaskClicked()
            } label: {
                Image(systemName: "trash")
            }
            Button {
                onEditTaskClicked()
            } label: {
                Image(systemName: "pencil")
            }
        }
    }
}

#Preview {
    TaskItem(task: Task(id: 1, text: "Task 1", userId: "", timestamp: 723123458, completed: false, notificationSent: false))
        .preferredColorScheme(.dark)
}

//
//  MainScreenEvent.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 15/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared

enum MainScreenEvent {
    case NextMonthBtnClicked
    case PreviousMonthBtnClicked
    case CalendarViewSwitch
    case CalendarDayClicked(dayIndex: Int)
    case EditTaskClicked(task: Task)
    case DeleteTaskClicked(task: Task)
    case TaskStatusChangeClicked(task: Task)
    case MessageDialogDismissed
    case EditTaskBottomSheetDismissed
    case SaveUpdatedTaskClicked(updatedTask: Task)
    case RecordNewTaskBtnClicked
    case NewTaskRecordingFinished(recognizedRequest: String)
    case NewTaskRecordingDismissed
    case RecognizedTaskTextEdited(updatedText: String)
    case CreateNewTaskClicked
}

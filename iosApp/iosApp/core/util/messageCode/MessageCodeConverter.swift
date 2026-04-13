//
//  MessageCodeConverter.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

class MessageCodes {
    static let loggingIn = 1
    static let gotErrorLoggingInCheckYourCred = 2
    static let gotUnexpectedErrorLoggingIn = 3
    static let registering = 4
    static let gotUnexpectedErrorSigningUp = 5
    static let gotErrorUserAlreadyExists = 6
    static let fieldsCannotBeEmpty = 7
    static let sendingNewPassword = 8
    static let gotUnexpectedErrorSendingNewPassword = 9
    static let sentNewPassword = 10
    static let userWithEmailDoesntExist = 11
    
    static let errorGettingNextMonth = 12
    static let errorGettingPreviousMonth = 13
    static let loadingTasks = 14
    static let errorLoadingTasks = 15
    static let errorFailedToGetLocalUserId = 16
    static let updatingTaskStatus = 17
    static let errorUpdatingTaskStatus = 18
    static let taskSuccessfullyUpdated = 19
    
    static let deletingTask = 20
    static let errorDeletingTask = 21
    static let taskSuccessfullyDeleted = 22
    static let errorUpdatingTask = 23
}

func getMessage(messageCode: Int?) -> String {
    return switch messageCode {
        case MessageCodes.loggingIn:
            NSLocalizedString("logging_in", comment: "")
        case MessageCodes.gotErrorLoggingInCheckYourCred:
            NSLocalizedString("error_logging_in_check_your_cred", comment: "")
        case MessageCodes.fieldsCannotBeEmpty:
            NSLocalizedString("fields_cannot_be_empty", comment: "")
        case MessageCodes.userWithEmailDoesntExist:
            NSLocalizedString("email_not_exists", comment: "")
        case MessageCodes.errorGettingNextMonth:
            NSLocalizedString("error_getting_next_month", comment: "")
        case MessageCodes.errorGettingPreviousMonth:
            NSLocalizedString("error_getting_previous_month", comment: "")
        case MessageCodes.loadingTasks:
            NSLocalizedString("loading_tasks", comment: "")
        case MessageCodes.updatingTaskStatus:
            NSLocalizedString("updating_task_status", comment: "")
        case MessageCodes.errorUpdatingTaskStatus:
            NSLocalizedString("error_updating_task_status", comment: "")
        case MessageCodes.taskSuccessfullyUpdated:
            NSLocalizedString("task_successfully_updated", comment: "")
        case MessageCodes.deletingTask:
            NSLocalizedString("deleting_task", comment: "")
        case MessageCodes.errorDeletingTask:
            NSLocalizedString("error_deleting_task", comment: "")
        case MessageCodes.taskSuccessfullyDeleted:
            NSLocalizedString("task_deleted", comment: "")
        default: ""
    }
}

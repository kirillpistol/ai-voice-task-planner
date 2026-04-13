//
//  IdentifiableTask.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 20/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import Shared

struct IdentifiableTask: Identifiable {
    let id: Int
    let task: Task
}

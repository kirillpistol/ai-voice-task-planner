//
//  CalendarItem.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 10/12/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct CalendarItem: View {
    
    let text: String
    let textColor: Color
    var isSelected: Bool
    var tasksAmount: Int
    var onSelected: () -> Void
    
    init(text: String, textColor: Color, isSelected: Bool, tasksAmount: Int, onSelected: @escaping () -> Void) {
        self.text = text
        self.textColor = textColor
        self.isSelected = isSelected
        self.tasksAmount = tasksAmount
        self.onSelected = onSelected
    }
    init(text: String, isSelected: Bool, tasksAmount: Int, onSelected: @escaping () -> Void) {
        self.text = text
        self.textColor = Color("TextColor")
        self.isSelected = isSelected
        self.tasksAmount = tasksAmount
        self.onSelected = onSelected
    }
    init(text: String, isSelected: Bool, tasksAmount: Int) {
        self.text = text
        self.textColor = Color("TextColor")
        self.isSelected = isSelected
        self.tasksAmount = tasksAmount
        self.onSelected = {}
    }
    init(text: String, textColor: Color) {
        self.text = text
        self.textColor = textColor
        self.isSelected = false
        self.tasksAmount = 0
        self.onSelected = {}
    }
    
    var body: some View {
        let backgroundColor = (isSelected) ? Color("Primary") : Color.clear
        let textColor = (isSelected) ? Color.white : textColor
        let taskIndicatorColor = (isSelected) ? Color.white : Color("Primary")
        
        ZStack {
            VStack {
                Text(text)
                    .foregroundColor(textColor)
            }
            VStack {
                HStack(spacing: 3) {
                    ForEach(0..<((tasksAmount>3) ? 3 : tasksAmount), id: \.self) { _ in
                        Circle()
                            .fill(taskIndicatorColor)
                            .frame(width: 4, height: 4)
                    }
                }.padding(.top, 25)
            }
        }
        .fixedSize()
        .frame(width: 45, height: 45)
        .background(backgroundColor)
        .cornerRadius(10)
        .onTapGesture {
            onSelected()
        }
    }
}

#Preview {
    HStack {
        CalendarItem(text: "11", isSelected: true, tasksAmount: 11)
        CalendarItem(text: "12", isSelected: true, tasksAmount: 2)
        CalendarItem(text: "13", isSelected: true, tasksAmount: 0)
    }
}

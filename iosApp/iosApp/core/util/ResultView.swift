//
//  ResultView.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared

struct ResultView: View {
    
    var resultType: ResultType
    var messageCode: Int
    var onDismiss: () -> Void
    private var imageName: String
    private var imageTint: Color
    
    init(resultType: ResultType, messageCode: Int, onDismiss: @escaping () -> Void) {
        self.resultType = resultType
        self.messageCode = messageCode
        self.onDismiss = onDismiss
        
        self.imageName = ""
        self.imageName = switch resultType {
        case .success: "checkmark"
        case .failure: "xmark"
        }
        self.imageTint = switch resultType {
        case .success: .green
        case .failure: .red
        }
    }
    
    var body: some View {
        VStack {
            Image(systemName: self.imageName)
                .resizable()
                .frame(width: 100, height: 100)
                .tint(imageTint)
            
            Spacer()
                .frame(height: 20)
            
            Text(getMessage(messageCode: self.messageCode))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 50)
            
            Spacer()
                .frame(height: 10)
            
            Button(
                action: { onDismiss() }
            ) {
                Text("OK")
            }.padding(.top, 40)
        }
    }
}

#Preview {
    ResultView(
        resultType: ResultType.failure,
        messageCode: MessageCodes.gotErrorLoggingInCheckYourCred,
        onDismiss: {}
    )
}

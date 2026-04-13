//
//  PasswordView.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 07/11/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct PasswordView: View {
    
    @Binding var password: String
    @State var isShowingPassword: Bool = false
    @FocusState var isFieldFocus: FieldToFocus?
    
    var body: some View {
        HStack {
            Group {
                if isShowingPassword {
                    TextField(
                        NSLocalizedString("password", comment: ""),
                        text: $password
                    )
                } else {
                    SecureField(
                        NSLocalizedString("password", comment: ""),
                        text: $password
                    )
                }
            }
            .disableAutocorrection(true)
            .autocapitalization(.none)
            .padding()
            
            Button {
                isShowingPassword.toggle()
            } label: {
                if isShowingPassword {
                    Image(systemName: "eye")
                } else {
                    Image(systemName: "eye.slash")
                }
            }.padding(.trailing, 10)
                .tint(.black)
        }
        .onChange(of: isShowingPassword) { result in
            isFieldFocus = isShowingPassword ? .textField : .secureField
        }
        .cornerRadius(10)
        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color("Stroke"), lineWidth: 1))
        .padding(.horizontal, 20)
    }
    
    enum FieldToFocus {
        case secureField, textField
    }
}

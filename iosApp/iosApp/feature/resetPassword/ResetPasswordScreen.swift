//
//  ResetPasswordScreen.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 13/11/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct ResetPasswordScreen: View {
    
    @EnvironmentObject var router: Router
    @ObservedObject var viewModel: ResetPasswordScreenViewModel
    
    var body: some View {
        ResetPasswordScreenContent(
            state: $viewModel.state,
            onEvent: { event in
                viewModel.onEvent(event: event)
            }
        )
    }
}

private struct ResetPasswordScreenContent: View {
    
    @Binding var state: ResetPasswordScreenState
    var onEvent: (ResetPasswordScreenEvent) -> Void
    
    var body: some View {
        VStack {
            switch state.screenState {
            case .idle:
                header
                    .padding(.top, 10)
                
                Spacer()
                
                fieldsSection
                
                Spacer()
                
                footer
            case .loading(let messageCode):
                Spacer()
                LoadingView(loading: true, message: getMessage(messageCode: messageCode))
                Spacer()
            case .result(let resultType, let messageCode):
                Spacer()
                ResultView(
                    resultType: resultType,
                    messageCode: messageCode,
                    onDismiss: { onEvent(ResetPasswordScreenEvent.MessageDialogDismissed) }
                )
                Spacer()
            }
        }
    }
    
    private var header: some View {
        HStack {
            Button {
                
            } label: {
                
            }
            
            Text(NSLocalizedString("password_reset", comment: ""))
                .foregroundColor(.white)
                .font(.title)
                .fontWeight(.bold)
            
            Spacer()
        }
        .padding(.horizontal, 30)
        .padding(.vertical, 20)
        .background(Color("OnBackgroundSurface"))
        .cornerRadius(15)
        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)
        .padding(.horizontal)
    }
    
    private var fieldsSection: some View {
        VStack(alignment: .leading) {
            Text(NSLocalizedString("enter_your_email", comment: ""))
                .fontWeight(.bold)
            Text(NSLocalizedString("will_send_there_password", comment: ""))
                .fontWeight(.light)
                .foregroundColor(Color("Hint"))
            
            TextField(
                NSLocalizedString("email", comment: ""),
                text: $state.emailState.text
            )
            .padding(15)
            .textInputAutocapitalization(.never)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color("Stroke"), lineWidth: 1))
        }
        .padding(.horizontal, 30)
    }
    
    private var footer: some View {
        Button {
            onEvent(ResetPasswordScreenEvent.SendBtnClicked)
        } label: {
            Text(NSLocalizedString("send", comment: ""))
                .foregroundColor(.white)
        }
        .padding(.horizontal, 100)
        .padding(.vertical, 15)
        .background(Color("Primary"))
        .cornerRadius(10)
        .padding(.bottom, 15)
    }
}

#Preview {
    ResetPasswordScreenContent(
        state: .constant(ResetPasswordScreenState()),
        onEvent: {_ in })
}

//
//  RegisterView.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 28/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct RegisterScreen: View {
    
    @EnvironmentObject var router: Router
    @ObservedObject var viewModel: RegisterScreenViewModel
    
    var body: some View {
        RegisterScreenContent(
            state: $viewModel.state,
            onEvent: { event in
                viewModel.onEvent(event: event)
            }
        )
    }
}

private struct RegisterScreenContent: View {
    
    @Binding var state: RegisterScreenState
    var onEvent: (RegisterScreenEvent) -> Void
    
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
                    onDismiss: { onEvent(RegisterScreenEvent.MessageDialogDismissed) }
                )
                Spacer()
            }
            
        }
    }
    
    private var header: some View {
        HStack {
            Image("TimeFlameIcon")
                .resizable()
                .frame(width: 100, height: 100)
            
            Spacer()
            
            VStack {
                Text("\(NSLocalizedString("create", comment: ""))\n\(NSLocalizedString("account", comment: ""))")
                    .foregroundColor(.white)
                    .font(.title)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                Text(NSLocalizedString("in_timeflame", comment: ""))
                    .foregroundColor(.white)
                    .opacity(0.8)
                
                Spacer().frame(height: 20)
                
                Text(NSLocalizedString("will_take_30_sec", comment: ""))
                    .foregroundColor(.white)
            }
        }
        .padding(.horizontal, 30)
        .padding(.vertical, 20)
        .background(Color("OnBackgroundSurface"))
        .cornerRadius(15)
        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)
        .padding(.horizontal)
    }
    private var fieldsSection: some View {
        VStack(spacing: 10) {
            TextField(
                NSLocalizedString("username", comment: ""),
                text: $state.usernameState.text
            )
            .padding(15)
            .textInputAutocapitalization(.never)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color("Stroke"), lineWidth: 1))
            .padding(.horizontal, 20)
            
            TextField(
                NSLocalizedString("email", comment: ""),
                text: $state.emailState.text
            )
            .padding(15)
            .textInputAutocapitalization(.never)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color("Stroke"), lineWidth: 1))
            .padding(.horizontal, 20)
            
            PasswordView(
                password: $state.passwordState.text
            )
        }
    }
    private var footer: some View {
        VStack {
            Button {
                onEvent(RegisterScreenEvent.RegisterBtnClicked)
            } label: {
                Text(NSLocalizedString("register", comment: ""))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 100)
            .padding(.vertical, 15)
            .background(Color("Primary"))
            .cornerRadius(10)
            
            Divider().padding(.horizontal, 100).padding(.top, 20)
            
            Button {
                onEvent(RegisterScreenEvent.ToLoginClicked)
            } label: {
                HStack {
                    Text(NSLocalizedString("have_account", comment: "")).foregroundColor(.black)
                    Text(NSLocalizedString("login", comment: "")).foregroundColor(Color("Primary"))
                }
            }
        }
    }
}

#Preview {
    RegisterScreenContent(
        state: .constant(RegisterScreenState(
            emailState: .init(text: ""),
            passwordState: .init(text: "")
        )),
        onEvent: { _ in }
    )
}

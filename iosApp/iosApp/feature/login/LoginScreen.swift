//
//  LoginView.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 28/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI
import Shared
import Foundation

struct LoginScreen: View {
    
    @EnvironmentObject var router: Router
    @ObservedObject var viewModel: LoginScreenViewModel = LoginScreenViewModel(onNavigate: { _ in})
    
    var body: some View {
        VStack {
            header
                .padding(.top, 10)
            
            
            switch viewModel.state.screenState {
            case .idle:
                Spacer()
                
                fieldsSection
                
                Spacer()
                
                footerSection
    
            case .loading(let messageCode):
                Spacer()
                LoadingView(loading: true, message: getMessage(messageCode: messageCode))
                Spacer()
                
            case .result(let resultType, let messageCode):
                Spacer()
                ResultView(
                    resultType: resultType,
                    messageCode: messageCode,
                    onDismiss: { viewModel.onEvent(event: LoginScreenEvent.MessageDialogDismissed) }
                )
                Spacer()
            }
            
        }
    }
    
    private var header: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(NSLocalizedString("welcome_to", comment: ""))
                    .foregroundColor(Color.white)
                    .padding(.bottom, 30)
                Image("Arrow")
                Spacer()
            }
            HStack {
                Spacer()
                Text("TimeFlame")
                    .foregroundColor(Color.white)
                    .padding(.bottom, 30)
                    .font(.title)
                    .fontWeight(.bold)
                
                Image("Arrow")
                    .padding(.top, 20)
                Spacer()
            }
            HStack {
                Spacer()
                Text(NSLocalizedString("log_into_account", comment: "")).foregroundColor(Color.white)
            }
        }
        .padding()
        .background(Color("OnBackgroundSurface"))
        .cornerRadius(15)
        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)
        .padding(.horizontal)
    }
    private var fieldsSection: some View {
        VStack(spacing: 10) {
            TextField(
                NSLocalizedString("email", comment: ""),
                text: $viewModel.state.emailState.text
            )
            .padding(15)
            .textInputAutocapitalization(.never)
            .cornerRadius(10)
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color("Stroke"), lineWidth: 1))
            .padding(.horizontal, 20)
            
            PasswordView(
                password: $viewModel.state.passwordState.text
            )
        
            HStack {
                Button {
                    viewModel.onEvent(event: LoginScreenEvent.ResetPasswordClicked)
                } label: {
                    Text(
                        NSLocalizedString("forgot_password_q", comment: "")
                    ).foregroundColor(Color("Primary"))
                }
                Spacer()
            }.padding(.leading, 20)
        }
    }
    private var footerSection: some View {
        VStack {
            Button {
                viewModel.onEvent(event: LoginScreenEvent.LoginBtnClicked)
            } label: {
                Text(NSLocalizedString("login", comment: ""))
                    .foregroundColor(.white)
            }
            .padding(.horizontal, 100)
            .padding(.vertical, 15)
            .background(Color("Primary"))
            .cornerRadius(10)
            
            Divider().padding(.horizontal, 100).padding(.top, 20)
            
            Button {
                viewModel.onEvent(event: LoginScreenEvent.CreateAccountClicked)
            } label: {
                HStack {
                    Text(NSLocalizedString("no_account_q", comment: "")).foregroundColor(.black)
                    Text(NSLocalizedString("create", comment: "")).foregroundColor(Color("Primary"))
                }
            }
        }
    }
}

#Preview {
    LoginScreen()
}

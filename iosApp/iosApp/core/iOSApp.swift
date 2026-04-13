import SwiftUI
import Shared

@main
struct iOSApp: App {
    
    @ObservedObject var router = Router()
    
    init() {
        KoinInitializerKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            let loginViewModel = LoginScreenViewModel(onNavigate: { dest in router.navigate(to: dest)})
            NavigationStack(path: $router.navPath) {
                if loginViewModel.checkAuth() {
                    MainScreen()
                        .navigationDestination(for: Router.Destination.self) { destination in
                            switch destination {
                            case .register:
                                let viewModel = RegisterScreenViewModel(onNavigate: { dest in router.navigate(to: dest)})
                                RegisterScreen(viewModel: viewModel)
                            case .main:
                                MainScreen()
                                    .navigationBarBackButtonHidden(true)
                            case .resetPassword:
                                let viewModel = ResetPasswordScreenViewModel(onNavigate: { dest in router.navigate(to: dest)})
                                ResetPasswordScreen(viewModel: viewModel)
                            case .login:
                                LoginScreen(viewModel: loginViewModel)
                            }
                        }
                } else {
                    LoginScreen(viewModel: loginViewModel)
                        .navigationDestination(for: Router.Destination.self) { destination in
                            switch destination {
                            case .register:
                                let viewModel = RegisterScreenViewModel(onNavigate: { dest in router.navigate(to: dest)})
                                RegisterScreen(viewModel: viewModel)
                            case .main:
                                MainScreen()
                                    .navigationBarBackButtonHidden(true)
                            case .resetPassword:
                                let viewModel = ResetPasswordScreenViewModel(onNavigate: { dest in router.navigate(to: dest)})
                                ResetPasswordScreen(viewModel: viewModel)
                            case .login:
                                LoginScreen(viewModel: LoginScreenViewModel(onNavigate: { dest in router.navigate(to: dest)}))
                            }
                        }
                }
            }
            .environmentObject(router)
        }
    }
}

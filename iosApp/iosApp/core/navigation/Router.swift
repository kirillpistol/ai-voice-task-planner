import SwiftUI

final class Router: ObservableObject {
    public enum Destination: Codable, Hashable {
        case login
        case resetPassword
        case register
        case main
    }
    
    @Published var navPath = NavigationPath()
    
    func navigate(to destination: Destination) {
        navPath.append(destination)
    }
    
    func navigateBack() {
        navPath.removeLast()
    }
    
    func navigateToRoot() {
        navPath.removeLast(navPath.count)
    }
}

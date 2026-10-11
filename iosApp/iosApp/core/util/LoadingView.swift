//
//  LoadingView.swift
//  iosApp
//
//  Created by Mikhail Maevskii on 30/10/2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import SwiftUI

struct LoadingView: View {
    
    @State private var loading: Bool = false
    private let message: String
    
    init(loading: Bool, message: String) {
        self.loading = loading
        self.message = message
    }
    
    var body: some View {
        VStack {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: Color("Primary")))
                
            
            Text(message)
        }
    }
}

#Preview {
    LoadingView(
        loading: true,
        message: "Loading message"
    )
}

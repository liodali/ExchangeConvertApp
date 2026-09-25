import SwiftUI
import SharedKMP

struct ContentView: View {
    var body: some View {
        // Shared Compose Multiplatform app (same UI code as Android).
        ComposeViewControllerProvider()
            .ignoresSafeArea()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

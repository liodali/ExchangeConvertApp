import SwiftUI
import shared

struct ContentView: View {
    @StateObject private var viewModel = CurrencyViewModel()
    
    var body: some View {
        NavigationView {
            CurrencyConverterView(
                viewModel: viewModel
            )
            .navigationTitle("Currency Converter")
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}

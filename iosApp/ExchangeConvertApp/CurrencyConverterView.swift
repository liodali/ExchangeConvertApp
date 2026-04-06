import SwiftUI
import shared

struct CurrencyConverterView: View {
    @ObservedObject var viewModel: CurrencyViewModel
    
    var body: some View {
        VStack(spacing: 16) {
            // Amount Input
            TextField("Amount", text: $viewModel.amount)
                .keyboardType(.decimalPad)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .padding(.horizontal)
            
            // From Currency Selection
            Picker("From", selection: $viewModel.selectedFromCurrency) {
                Text("Select Currency").tag(nil as Currency?)
                ForEach(viewModel.currencies, id: \.name) { currency in
                    Text("\(currency.name) - \(currency.fullCountryName)").tag(currency as Currency?)
                }
            }
            .pickerStyle(MenuPickerStyle())
            .padding(.horizontal)
            
            // Swap Button
            Button(action: {
                viewModel.swapCurrencies()
            }) {
                Image(systemName: "arrow.up.arrow.down")
                    .font(.title2)
            }
            .padding()
            
            // To Currency Selection
            Picker("To", selection: $viewModel.selectedToCurrency) {
                Text("Select Currency").tag(nil as Currency?)
                ForEach(viewModel.currencies, id: \.name) { currency in
                    Text("\(currency.name) - \(currency.fullCountryName)").tag(currency as Currency?)
                }
            }
            .pickerStyle(MenuPickerStyle())
            .padding(.horizontal)
            
            // Calculate Button
            Button(action: {
                viewModel.calculateRates()
            }) {
                Text("Convert")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)
            
            // Loading Indicator
            if viewModel.isLoading {
                ProgressView("Loading...")
                    .padding()
            }
            
            // Error Message
            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .foregroundColor(.red)
                    .padding()
            }
            
            Spacer()
        }
        .onAppear {
            viewModel.loadCurrencies()
        }
    }
}

struct CurrencyConverterView_Previews: PreviewProvider {
    static var previews: some View {
        CurrencyConverterView(viewModel: CurrencyViewModel())
    }
}

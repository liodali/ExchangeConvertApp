import SwiftUI
import SharedKMP

/// CurrencyConverterView - Main SwiftUI view for currency conversion
struct CurrencyConverterView: View {
    @StateObject var viewModel = CurrencyViewModel()
    @State private var amount: String = ""
    @State private var showFromPicker = false
    @State private var showToPicker = false
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Amount Input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Amount")
                        .font(.headline)
                    TextField("Enter amount", text: $amount)
                        .keyboardType(.decimalPad)
                        .padding()
                        .background(Color.gray.opacity(0.1))
                        .cornerRadius(8)
                }
                .padding(.horizontal)
                
                // Currency Selection
                HStack(spacing: 16) {
                    // From Currency
                    Button(action: { showFromPicker = true }) {
                        VStack {
                            Text("From")
                                .font(.caption)
                            Text(viewModel.selectedFromCurrency?.name ?? "USD")
                                .font(.headline)
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(8)
                    }
                    
                    // Swap Button
                    Button(action: {
                        viewModel.swapCurrencies()
                    }) {
                        Image(systemName: "arrow.left.arrow.right")
                            .font(.title2)
                    }
                    
                    // To Currency
                    Button(action: { showToPicker = true }) {
                        VStack {
                            Text("To")
                                .font(.caption)
                            Text(viewModel.selectedToCurrency?.name ?? "EUR")
                                .font(.headline)
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.blue.opacity(0.1))
                        .cornerRadius(8)
                    }
                }
                .padding(.horizontal)
                
                // Convert Button
                Button(action: {
                    viewModel.amount = amount
                    viewModel.calculateRates()
                }) {
                    Text("Convert")
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(8)
                }
                .padding(.horizontal)
                
                // Results
                if !viewModel.exchangeRates.isEmpty {
                    List(viewModel.exchangeRates, id: \.name) { rate in
                        HStack {
                            Text(rate.name)
                            Spacer()
                            Text(String(format: "%.4f", rate.rate))
                        }
                    }
                }
                
                // Error Message
                if let error = viewModel.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .padding()
                }
            }
            .navigationTitle("Currency Converter")
            .sheet(isPresented: $showFromPicker) {
                CurrencyPickerView(
                    currencies: viewModel.currencies,
                    selectedCurrency: viewModel.selectedFromCurrency,
                    onSelect: { currency in
                        viewModel.selectedFromCurrency = currency
                    }
                )
            }
            .sheet(isPresented: $showToPicker) {
                CurrencyPickerView(
                    currencies: viewModel.currencies,
                    selectedCurrency: viewModel.selectedToCurrency,
                    onSelect: { currency in
                        viewModel.selectedToCurrency = currency
                    }
                )
            }
        }
    }
}

#Preview {
    CurrencyConverterView()
}

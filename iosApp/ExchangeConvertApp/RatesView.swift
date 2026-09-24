import SwiftUI
import SharedKMP

/// RatesView - Main entry point for the Rates page
/// Displays:
/// - Header with currency selector
/// - Quick Exchange section (API-driven)
/// - History Graph section (mock data)
/// - Rates list (API-driven)
struct RatesView: View {
    @ObservedObject var viewModel: CurrencyViewModel
    @State private var showingCurrencyPicker = false
    @State private var pickerType: CurrencyPickerType = .from
    
    enum CurrencyPickerType {
        case from
        case to
        case base
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                DesignColors.background.ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 0) {
                        // Header with currency selector
                        headerSection
                        
                        // Quick Exchange Section
                        QuickExchangeSection(
                            baseCurrency: viewModel.baseCurrency,
                            rates: viewModel.ratesMap,
                            onConvertClick: { amount, from, to in
                                // Handle conversion
                                viewModel.convertAmount(amount, from: from, to: to)
                            },
                            onFromCurrencyClick: {
                                pickerType = .from
                                showingCurrencyPicker = true
                            },
                            onToCurrencyClick: {
                                pickerType = .to
                                showingCurrencyPicker = true
                            }
                        )
                        .padding(.horizontal, 16)
                        .padding(.top, 16)
                        
                        // History Graph Section (mock data)
                        HistoryGraphSection(
                            baseCurrency: viewModel.baseCurrency,
                            targetCurrency: "EUR" // TODO: Make selectable
                        )
                        .padding(.horizontal, 16)
                        .padding(.top, 16)
                        
                        // Rates Header
                        Text("Available Rates")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(DesignColors.onBackground)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 16)
                        
                        // Rates List (API-driven)
                        if viewModel.isLoading {
                            loadingView
                        } else if viewModel.errorMessage != nil {
                            errorView
                        } else if viewModel.exchangeRates.isEmpty {
                            emptyView
                        } else {
                            ratesList
                        }
                        
                        // Footer with last updated
                        Text("Last updated: Just now")
                            .font(.system(size: 12, weight: .regular))
                            .foregroundColor(DesignColors.onSurfaceMuted)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 24)
                    }
                }
            }
            .navigationTitle("Rates")
            .navigationBarTitleDisplayMode(.large)
            .sheet(isPresented: $showingCurrencyPicker) {
                CurrencyPickerView(
                    currencies: viewModel.currencies,
                    selectedCurrency: pickerType == .from ? viewModel.selectedFromCurrency : viewModel.selectedToCurrency,
                    onSelect: { currency in
                        if pickerType == .from {
                            viewModel.selectedFromCurrency = currency
                        } else {
                            viewModel.selectedToCurrency = currency
                        }
                    }
                )
            }
        }
        .onAppear {
            viewModel.loadCurrencies()
            viewModel.loadRates()
        }
    }
    
    // MARK: - Header Section
    private var headerSection: some View {
        VStack(spacing: 16) {
            Text("Exchange Rates")
                .font(.system(size: 36, weight: .bold))
                .foregroundColor(DesignColors.onBackground)
            
            Text("Live rates based on \(viewModel.baseCurrency) base currency")
                .font(.system(size: 18, weight: .regular))
                .foregroundColor(DesignColors.onSurface)
                .multilineTextAlignment(.center)
            
            // Currency Selector Button
            Button(action: {
                showingCurrencyPicker = true
                pickerType = .base
            }) {
                HStack(spacing: 8) {
                    // Flag placeholder (circle with currency code)
                    ZStack {
                        Circle()
                            .fill(DesignColors.buttonBackground)
                            .frame(width: 24, height: 24)
                        
                        Text(viewModel.baseCurrency.prefix(1))
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(DesignColors.onBackground)
                    }
                    
                    Text("\(viewModel.baseCurrency) Base")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(DesignColors.onBackground)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(
                    Capsule()
                        .fill(DesignColors.buttonBackground)
                        .overlay(
                            Capsule()
                                .stroke(DesignColors.stroke.opacity(0.15), lineWidth: 1)
                        )
                )
            }
        }
        .padding(.horizontal, 24)
        .padding(.top, 16)
    }
    
    // MARK: - Loading View
    private var loadingView: some View {
        HStack {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: DesignColors.onSurfaceMuted))
            Text("Loading rates...")
                .foregroundColor(DesignColors.onSurfaceMuted)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
    
    // MARK: - Error View
    private var errorView: some View {
        VStack(spacing: 8) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 32))
                .foregroundColor(DesignColors.error)
            
            Text(viewModel.errorMessage ?? "Unknown error")
                .foregroundColor(DesignColors.onSurfaceMuted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
    }
    
    // MARK: - Empty View
    private var emptyView: some View {
        Text("No rates available")
            .foregroundColor(DesignColors.onSurfaceMuted)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 40)
    }
    
    // MARK: - Rates List
    private var ratesList: some View {
        LazyVStack(spacing: 0) {
            ForEach(viewModel.exchangeRates, id: \.name) { rate in
                RateCardView(
                    currencyCode: rate.name,
                    currencyName: getCurrencyName(rate.name),
                    rateValue: rate.calculatedAmount,
                    changePercent: nil // TODO: Calculate from historic data
                )
                .padding(.horizontal, 16)
            }
        }
        .padding(.bottom, 16)
    }
    
    // MARK: - Helper Functions
    private func getCurrencyName(_ code: String) -> String {
        switch code.uppercased() {
        case "EUR": return "Euro"
        case "GBP": return "British Pound"
        case "JPY": return "Japanese Yen"
        case "CHF": return "Swiss Franc"
        case "CAD": return "Canadian Dollar"
        case "AUD": return "Australian Dollar"
        case "USD": return "US Dollar"
        default: return code
        }
    }
}

/// Currency Picker View
struct CurrencyPickerView: View {
    let currencies: [Currency]
    let selectedCurrency: Currency?
    let onSelect: (Currency) -> Void
    
    @Environment(\.dismiss) var dismiss
    @State private var searchText = ""
    
    var filteredCurrencies: [Currency] {
        if searchText.isEmpty {
            return currencies
        }
        return currencies.filter {
            $0.name.localizedCaseInsensitiveContains(searchText) ||
            $0.fullCountryName.localizedCaseInsensitiveContains(searchText)
        }
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search Bar
                TextField("Search currencies", text: $searchText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding()
                
                List(filteredCurrencies, id: \.name) { currency in
                    Button(action: {
                        onSelect(currency)
                        dismiss()
                    }) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(currency.name)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(DesignColors.onBackground)
                                Text(currency.fullCountryName)
                                    .font(.system(size: 12, weight: .regular))
                                    .foregroundColor(DesignColors.onSurfaceMuted)
                            }
                            
                            Spacer()
                            
                            if currency.name == selectedCurrency?.name {
                                Image(systemName: "checkmark")
                                    .foregroundColor(DesignColors.success)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }
                .listStyle(.plain)
            }
            .navigationTitle("Select Currency")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .foregroundColor(DesignColors.onSurfaceMuted)
                }
            }
        }
    }
}

#Preview {
    RatesView(viewModel: CurrencyViewModel())
}

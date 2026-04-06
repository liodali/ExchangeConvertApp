import Foundation
import shared
import Combine

class CurrencyViewModel: ObservableObject {
    @Published var currencies: [Currency] = []
    @Published var exchangeRates: [ExchangeRate] = []
    @Published var selectedFromCurrency: Currency?
    @Published var selectedToCurrency: Currency?
    @Published var amount: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    private var repository: IRepository?
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        setupRepository()
        loadCurrencies()
    }
    
    private func setupRepository() {
        // Initialize the shared repository
        // This will be implemented when we create the DI setup
        print("Setting up repository...")
    }
    
    func loadCurrencies() {
        isLoading = true
        
        // Call shared KMP module
        Task {
            do {
                // This is a placeholder - actual implementation will use the shared repository
                try await Task.sleep(nanoseconds: 1_000_000_000)
                
                // Sample data for now
                await MainActor.run {
                    self.currencies = [
                        Currency(name: "USD", fullCountryName: "United States"),
                        Currency(name: "EUR", fullCountryName: "European Union"),
                        Currency(name: "GBP", fullCountryName: "United Kingdom")
                    ]
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                }
            }
        }
    }
    
    func swapCurrencies() {
        let temp = selectedFromCurrency
        selectedFromCurrency = selectedToCurrency
        selectedToCurrency = temp
    }
    
    func calculateRates() {
        guard let amountValue = Double(amount),
              let fromCurrency = selectedFromCurrency,
              let toCurrency = selectedToCurrency else {
            return
        }
        
        // Call shared KMP module to calculate rates
        print("Calculating rates for \(amountValue) \(fromCurrency.name) to \(toCurrency.name)")
    }
}

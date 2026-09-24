import Foundation
import SharedKMP
import Combine

class CurrencyViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var currencies: [Currency] = []
    @Published var exchangeRates: [ExchangeRate] = []
    @Published var selectedFromCurrency: Currency?
    @Published var selectedToCurrency: Currency?
    @Published var amount: String = ""
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    @Published var baseCurrency: String = "USD"
    @Published var ratesMap: [String: Double] = [:]
    
    // MARK: - Private Properties
    private var repository: IRepository?
    private var cancellables = Set<AnyCancellable>()

    // MARK: - Initialization
    init() {
        setupRepository()
        loadCurrencies()
        loadRates()
    }

    // MARK: - Repository Setup
    private func setupRepository() {
        // Initialize the shared KMP repository with the exported iOS HttpClient factory.
        let serverURL = "api.openexchangerate.com"
        let httpClient = HttpClientFactory_iosKt.createHttpClient(serverURL: serverURL)
        let currencyApi = CurrencyApi(httpClient: httpClient)
        self.repository = CurrencyRepositoryImpl(currencyApi: currencyApi)
    }

    func loadCurrencies() {
        isLoading = true

        guard let repository = repository else {
            errorMessage = "Repository not initialized"
            isLoading = false
            return
        }

        // Call shared KMP module repository
        Task {
            do {
                // Use Kotlin coroutines from the shared module
                let response = try await repository.getListCurrencies()

                await MainActor.run {
                    if let successResponse = response as? MyResponseSuccess<NSArray> {
                        self.currencies = (successResponse.data as? [Any])?.compactMap {
                            $0 as? Currency
                        } ?? []
                        self.isLoading = false
                    } else if let errorResponse = response as? MyResponseError {
                        self.errorMessage = "Failed to load currencies: \(errorResponse.error)"
                        self.isLoading = false
                    } else {
                        self.errorMessage = "Unknown response type"
                        self.isLoading = false
                    }
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
            errorMessage = "Please enter amount and select currencies"
            return
        }

        // Call shared KMP module to calculate rates
        Task {
            guard let repository = repository else {
                await MainActor.run {
                    self.errorMessage = "Repository not initialized"
                }
                return
            }

            do {
                let response = try await repository.getListRatesCurrencies(amount: amountValue)

                await MainActor.run {
                    if let successResponse = response as? MyResponseSuccess<NSArray> {
                        self.exchangeRates = (successResponse.data as? [Any])?.compactMap {
                            $0 as? ExchangeRate
                        } ?? []
                    } else if let errorResponse = response as? MyResponseError {
                        self.errorMessage = "Failed to calculate rates: \(errorResponse.error)"
                    }
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                }
            }
        }
    }
    
    // MARK: - Public Methods
    
    /// Load latest exchange rates from API
    func loadRates() {
        isLoading = true
        
        guard let repository = repository else {
            errorMessage = "Repository not initialized"
            isLoading = false
            return
        }
        
        Task {
            do {
                // Get latest rates for base currency
                let response = try await repository.getListRatesCurrencies(amount: 1.0)
                
                await MainActor.run {
                    if let successResponse = response as? MyResponseSuccess<NSArray> {
                        self.exchangeRates = (successResponse.data as? [Any])?.compactMap {
                            $0 as? ExchangeRate
                        } ?? []
                        
                        // Build rates map for quick conversion
                        self.buildRatesMap()
                        self.isLoading = false
                    } else if let errorResponse = response as? MyResponseError {
                        self.errorMessage = "Failed to load rates: \(errorResponse.error)"
                        self.isLoading = false
                    }
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                }
            }
        }
    }
    
    /// Build rates map from exchange rates for quick conversion
    private func buildRatesMap() {
        var rates: [String: Double] = [:]
        for rate in exchangeRates {
            rates[rate.name] = rate.rate
        }
        self.ratesMap = rates
    }
    
    /// Convert amount between currencies
    func convertAmount(_ amount: Double, from: String, to: String) {
        // Formula: amount * (rates[toCurrency] / rates[fromCurrency])
        let fromRate = from == baseCurrency ? 1.0 : (ratesMap[from] ?? 1.0)
        let toRate = to == baseCurrency ? 1.0 : (ratesMap[to] ?? 1.0)
        let convertedAmount = amount * (toRate / fromRate)
        print("Converted \(amount) \(from) to \(convertedAmount) \(to)")
        // TODO: Show result or navigate to result screen
    }
    
    /// Set base currency and reload rates
    func setBaseCurrency(_ currency: String) {
        baseCurrency = currency
        loadRates()
    }
}

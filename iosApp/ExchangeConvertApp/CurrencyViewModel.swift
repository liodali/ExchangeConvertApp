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
        // Initialize the shared KMP repository with the exported iOS HttpClient factory.
        let serverURL = "https://api.currencyapi.com/"
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
}

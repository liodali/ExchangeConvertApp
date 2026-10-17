import AppIntents
import SharedKMP

// MARK: - Convert Currency

/// "Convert 250 EUR to USD" — live conversion via the shared repository.
@available(iOS 16.0, *)
struct ConvertCurrencyIntent: AppIntent {
    static var title: LocalizedStringResource = "Convert Currency"
    static var description = IntentDescription(
        "Convert an amount between currencies using live exchange rates (fiat, gold, silver, crypto)."
    )

    @Parameter(title: "Amount", default: 100)
    var amount: Double

    // Note: @Parameter(default:) must be a compile-time literal in Swift 5
    // mode — entity references aren't, so entity params are optional and
    // coalesced to defaults in perform().
    @Parameter(title: "From", requestValueDialog: "From which currency?")
    var from: CurrencyEntity?

    @Parameter(title: "To", requestValueDialog: "To which currency?")
    var to: CurrencyEntity?

    static var parameterSummary: some ParameterSummary {
        Summary("Convert \(\.$amount) from \(\.$from) to \(\.$to)")
    }

    init() {}

    func perform() async throws -> IntentResultContainer<Double, Never, Never, IntentDialog> {
        let fromCurrency = from ?? CurrencyEntity.eur
        let toCurrency = to ?? CurrencyEntity.usd
        let row = try await AppIntentsBridge.shared.convert(
            amount: amount, from: fromCurrency.id, to: toCurrency.id
        )
        if let error = row.errorMessage {
            throw IntentFailure(error)
        }
        return .result(
            value: row.convertedAmount,
            dialog: "\(fmtAmount(row.amount)) \(row.from) = \(fmtAmount(row.convertedAmount)) \(row.to) (rate \(fmtRate(row.rate)))"
        )
    }
}

// MARK: - Get Exchange Rate

/// "What's the EUR to USD rate?" — Convert with amount 1.
@available(iOS 16.0, *)
struct GetExchangeRateIntent: AppIntent {
    static var title: LocalizedStringResource = "Get Exchange Rate"
    static var description = IntentDescription(
        "The live exchange rate between two currencies (1 unit of the base)."
    )

    @Parameter(title: "From", requestValueDialog: "From which currency?")
    var from: CurrencyEntity?

    @Parameter(title: "To", requestValueDialog: "To which currency?")
    var to: CurrencyEntity?

    static var parameterSummary: some ParameterSummary {
        Summary("Rate from \(\.$from) to \(\.$to)")
    }

    init() {}

    func perform() async throws -> IntentResultContainer<Double, Never, Never, IntentDialog> {
        let fromCurrency = from ?? CurrencyEntity.eur
        let toCurrency = to ?? CurrencyEntity.usd
        let row = try await AppIntentsBridge.shared.convert(
            amount: 1, from: fromCurrency.id, to: toCurrency.id
        )
        if let error = row.errorMessage {
            throw IntentFailure(error)
        }
        return .result(
            value: row.rate,
            dialog: "1 \(row.from) = \(fmtRate(row.rate)) \(row.to)"
        )
    }
}

// MARK: - Rate History

/// "How did EUR/USD move this month?" — min/max/Δ% over a day window.
@available(iOS 16.0, *)
struct GetRateHistoryIntent: AppIntent {
    static var title: LocalizedStringResource = "Get Rate History"
    static var description = IntentDescription(
        "Historical trend of a currency pair: latest rate, change percent, min and max over the last days."
    )

    @Parameter(title: "Base", requestValueDialog: "Which base currency?")
    var base: CurrencyEntity?

    @Parameter(title: "Quote", requestValueDialog: "Against which currency?")
    var quote: CurrencyEntity?

    @Parameter(title: "Days", default: 30)
    var days: Int

    static var parameterSummary: some ParameterSummary {
        Summary("History of \(\.$base) to \(\.$quote) over \(\.$days) days")
    }

    init() {}

    func perform() async throws -> IntentResultContainer<Double, Never, Never, IntentDialog> {
        let baseCurrency = base ?? CurrencyEntity.eur
        let quoteCurrency = quote ?? CurrencyEntity.usd
        let window = max(1, min(days, 365))
        let calendar = Calendar(identifier: .iso8601)
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.locale = Locale(identifier: "en_US_POSIX")
        let today = Date()
        let to = formatter.string(from: today)
        let from = formatter.string(
            from: calendar.date(byAdding: .day, value: -(window - 1), to: today) ?? today
        )
        let history = try await AppIntentsBridge.shared.history(
            base: baseCurrency.id, quote: quoteCurrency.id, from: from, to: to
        )
        if let error = history.errorMessage {
            throw IntentFailure(error)
        }
        let direction = history.changePercent >= 0 ? "up" : "down"
        return .result(
            value: history.changePercent,
            dialog: "\(history.base)/\(history.quote) over \(window) days: \(direction) \(String(format: "%.2f", abs(history.changePercent)))% — now \(fmtRate(history.latestRate)) (min \(fmtRate(history.minRate)), max \(fmtRate(history.maxRate)))"
        )
    }
}

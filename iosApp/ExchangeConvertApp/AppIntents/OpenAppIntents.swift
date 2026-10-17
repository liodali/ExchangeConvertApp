import AppIntents
import SharedKMP

// The iOS counterpart of Android's `openAppScreen` AppFunction: instead of a
// PendingIntent, AppIntents stash a deep link into the shared `DeepLinkNav`
// and let `openAppWhenRun` bring the app forward. The Compose NavHost
// consumes the pending link after composition — works on cold launch AND on
// a warm app (same path the widgets' sovereign:// URLs use).

@available(iOS 16.0, *)
enum AppScreenOption: String, AppEnum {
    case home
    case converter
    case fullConverter = "full-converter"
    case history
    case rateAlerts = "rate-alerts"
    case widgets
    case account

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Screen"

    static let caseDisplayRepresentations: [AppScreenOption: DisplayRepresentation] = [
        .home: "Home",
        .converter: "Converter",
        .fullConverter: "Full converter",
        .history: "History",
        .rateAlerts: "Rate alerts",
        .widgets: "Widgets",
        .account: "Account",
    ]
}

/// Opens the app on a screen; the converter accepts a prefilled pair and
/// amount (the app converts automatically), history preselects the pair.
@available(iOS 16.0, *)
struct OpenAppScreenIntent: AppIntent {
    static var title: LocalizedStringResource = "Open Sovereign Ledger"
    static var description = IntentDescription(
        "Opens the app on a chosen screen — the converter can start from a pair and an amount."
    )
    // Brings the app forward after perform() returns (Siri/Shortcuts "open app").
    static var openAppWhenRun = true

    @Parameter(title: "Screen", default: AppScreenOption.fullConverter)
    var screen: AppScreenOption

    @Parameter(title: "From", requestValueDialog: "From which currency?")
    var from: CurrencyEntity?

    @Parameter(title: "To", requestValueDialog: "To which currency?")
    var to: CurrencyEntity?

    @Parameter(title: "Amount")
    var amount: Double?

    static var parameterSummary: some ParameterSummary {
        Summary("Open \(\.$screen) from \(\.$from) to \(\.$to), amount \(\.$amount)")
    }

    init() {}

    func perform() async throws -> IntentResultContainer<Never, Never, Never, IntentDialog> {
        // Stash the deep link BEFORE the app opens — the shared Compose
        // NavHost consumes it after composition (cold start AND warm app).
        DeepLinkNav.shared.stash(
            route: screen.rawValue,
            pairBase: (from ?? CurrencyEntity.eur).id,
            pairQuote: (to ?? CurrencyEntity.usd).id,
            amount: amount.map { fmtAmount($0) }
        )
        return .result(dialog: "Opening the \(screen.rawValue) screen.")
    }
}

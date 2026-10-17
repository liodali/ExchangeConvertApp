import AppIntents
import SharedKMP

// App Intents (iOS parity with Android AppFunctions — see
// plans/appintents-ios.md). The whole layer is @available(iOS 16.0, *)
// because the app target still deploys to iOS 15; AppIntents weak-links.
//
// Everything data-shaped funnels through the Kotlin `AppIntentsBridge`
// (shared/src/iosMain/.../platform/AppIntentsBridge.kt) → same repository,
// validation, and caps as the in-app flows and the Android service.

/// Formats a rate with digits that match its magnitude (same policy as the
/// widgets' rate text).
@available(iOS 16.0, *)
func fmtRate(_ value: Double) -> String {
    let decimals: Int
    switch value {
    case 100...: decimals = 2
    case 1..<100: decimals = 4
    case 0.01..<1: decimals = 5
    default: decimals = 7
    }
    return String(format: "%.\(decimals)f", value)
}

/// Formats a money-ish amount (2 decimals, thousands separators).
@available(iOS 16.0, *)
func fmtAmount(_ value: Double) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = value.truncatingRemainder(dividingBy: 1) == 0 ? 0 : 2
    return formatter.string(from: NSNumber(value: value)) ?? String(value)
}

/// Envelope errors from the Kotlin bridge surfaced as thrown intent errors
/// (Siri/Shortcuts show `errorDescription`).
@available(iOS 16.0, *)
struct IntentFailure: LocalizedError {
    let message: String
    var errorDescription: String? { message }

    init(_ envelope: String?) {
        message = envelope ?? "Something went wrong — please try again."
    }
}

/// Swift-side accessor for the shared currency catalog
/// (`CurrenciesCatalog` in commonMain). Kotlin `Currency.name` is the ISO
/// code and `fullCountryName` the display name (legacy naming).
@available(iOS 16.0, *)
enum CatalogUI {
    /// Curated picker suggestions (codes resolved against the catalog).
    static let suggestedCodes = [
        "USD", "EUR", "GBP", "CHF", "JPY", "CNY", "CAD", "AUD",
        "TND", "MAD", "SAR", "AED", "TRY", "SEK", "NOK",
        "XAU", "XAG", "BTC",
    ]

    /// All catalog entries mapped to entities (computed once).
    static let all: [CurrencyEntity] = {
        var out: [CurrencyEntity] = []
        for currency in CurrenciesCatalog.shared.currencies {
            out.append(CurrencyEntity(id: currency.name, name: currency.fullCountryName))
        }
        return out.sorted { $0.id < $1.id }
    }()

    static func entity(byCode code: String) -> CurrencyEntity? {
        let normalized = code.trimmingCharacters(in: .whitespaces).uppercased()
        return all.first { $0.id == normalized }
    }

    static func search(_ query: String) -> [CurrencyEntity] {
        all.filter {
            $0.id.localizedCaseInsensitiveContains(query) ||
                $0.name.localizedCaseInsensitiveContains(query)
        }
    }
}

/// A currency from the shared catalog — ISO code identity ("EUR", "XAU"…).
@available(iOS 16.0, *)
struct CurrencyEntity: AppEntity {
    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Currency"

    /// ISO-4217-style code, e.g. "EUR" (same string the bridge validates).
    let id: String
    /// Display name, e.g. "Euro".
    let name: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(name) (\(id))")
    }

    static var defaultQuery = CurrencyEntityQuery()

    // Sensible @Parameter(default:) anchors.
    static let usd = CurrencyEntity(id: "USD", name: "US Dollar")
    static let eur = CurrencyEntity(id: "EUR", name: "Euro")
    static let tnd = CurrencyEntity(id: "TND", name: "Tunisian Dinar")

    struct CurrencyEntityQuery: EntityQuery, EntityStringQuery {
        func suggestedEntities() async throws -> [CurrencyEntity] {
            CatalogUI.suggestedCodes.compactMap { CatalogUI.entity(byCode: $0) }
        }

        func entities(matching string: String) async throws -> [CurrencyEntity] {
            let query = string.trimmingCharacters(in: .whitespaces)
            guard !query.isEmpty else { return [] }
            return Array(CatalogUI.search(query).prefix(12))
        }

        func entities(for identifiers: [String]) async throws -> [CurrencyEntity] {
            identifiers.map { code in
                CatalogUI.entity(byCode: code) ?? CurrencyEntity(id: code.uppercased(), name: code.uppercased())
            }
        }

        func defaultResult() async throws -> CurrencyEntity? {
            CatalogUI.entity(byCode: "EUR")
        }
    }
}

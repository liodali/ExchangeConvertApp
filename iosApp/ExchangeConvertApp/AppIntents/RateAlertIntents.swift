import AppIntents
import SharedKMP

// MARK: - Alert enums

@available(iOS 16.0, *)
enum AlertModeOption: String, AppEnum {
    case periodic = "PERIODIC"
    case threshold = "THRESHOLD"

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Alert Type"

    static let caseDisplayRepresentations: [AlertModeOption: DisplayRepresentation] = [
        .periodic: "Periodic check",
        .threshold: "Threshold move",
    ]
}

@available(iOS 16.0, *)
enum AlertIntervalOption: Int, AppEnum {
    case hourly = 60
    case twoHours = 120

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Check Interval"

    static let caseDisplayRepresentations: [AlertIntervalOption: DisplayRepresentation] = [
        .hourly: "Every hour",
        .twoHours: "Every 2 hours",
    ]
}

// MARK: - Alert entity (listAlerts-backed, for the delete picker)

@available(iOS 16.0, *)
struct AlertEntity: AppEntity {
    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Rate Alert"

    /// Composite identity "<SOURCE>:<id>" (LOCAL rows are SQLDelight ids,
    /// SERVER rows are backend ids).
    let id: String
    let base: String
    let quote: String
    let mode: String
    let source: String

    var displayRepresentation: DisplayRepresentation {
        let kind = mode == "THRESHOLD" ? "threshold" : "periodic"
        let whereFrom = source == "SERVER" ? "pushed" : "on device"
        return DisplayRepresentation(title: "\(base) → \(quote)", subtitle: "\(kind), \(whereFrom)")
    }

    static var defaultQuery = AlertEntityQuery()

    struct AlertEntityQuery: EntityQuery {
        func suggestedEntities() async throws -> [AlertEntity] {
            let result = try await AppIntentsBridge.shared.listAlerts()
            guard let error = result.errorMessage else { return result.alerts.map(AlertEntity.init) }
            throw IntentFailure(error)
        }

        func entities(for identifiers: [String]) async throws -> [AlertEntity] {
            let all = try await suggestedEntities()
            return identifiers.compactMap { id in all.first { $0.id == id } }
        }

        func defaultResult() async throws -> AlertEntity? { nil }
    }

    init(row: AlertRow) {
        id = "\(row.source):\(row.alertId)"
        base = row.base
        quote = row.quote
        mode = row.mode
        source = row.source
    }

    init(id: String, base: String, quote: String, mode: String, source: String) {
        self.id = id
        self.base = base
        self.quote = quote
        self.mode = mode
        self.source = source
    }
}

// MARK: - Add Rate Alert

/// Creates an on-device alert (same caps as the app: 2 alerts, one per pair).
@available(iOS 16.0, *)
struct AddRateAlertIntent: AppIntent {
    static var title: LocalizedStringResource = "Add Rate Alert"
    static var description = IntentDescription(
        "Get a notification when a currency pair is checked — periodically, or when it moves beyond a threshold."
    )

    @Parameter(title: "Base", requestValueDialog: "Which base currency?")
    var base: CurrencyEntity?

    @Parameter(title: "Quote", requestValueDialog: "Against which currency?")
    var quote: CurrencyEntity?

    @Parameter(title: "Type", default: AlertModeOption.periodic)
    var mode: AlertModeOption

    @Parameter(title: "Interval", default: AlertIntervalOption.hourly)
    var interval: AlertIntervalOption

    @Parameter(title: "Move Threshold (%)", default: 0.5)
    var thresholdPercent: Double

    // NOTE: a conditional `When(\.$mode, .equals, …)` summary failed to
    // infer its key path on the Xcode 26.5 toolchain — plain summary until
    // that's revisited (see plans/appintents-ios.md gotchas).
    static var parameterSummary: some ParameterSummary {
        Summary("Alert \(\.$base) → \(\.$quote), type \(\.$mode), interval \(\.$interval), threshold \(\.$thresholdPercent)%")
    }

    init() {}

    func perform() async throws -> IntentResultContainer<Never, Never, Never, IntentDialog> {
        let baseCurrency = base ?? CurrencyEntity.eur
        let quoteCurrency = quote ?? CurrencyEntity.usd
        let result = try await AppIntentsBridge.shared.addAlert(
            base: baseCurrency.id,
            quote: quoteCurrency.id,
            mode: mode.rawValue,
            intervalMinutes: Int64(interval.rawValue),
            thresholdPercent: thresholdPercent
        )
        if let error = result.errorMessage {
            throw IntentFailure(error)
        }
        let kind = mode == .threshold
            ? "when it moves \(thresholdPercent)%"
            : "every \(interval == .hourly ? "hour" : "2 hours")"
        return .result(dialog: "Alert created for \(baseCurrency.id) → \(quoteCurrency.id), \(kind).")
    }
}

// MARK: - List Rate Alerts

@available(iOS 16.0, *)
struct ListRateAlertsIntent: AppIntent {
    static var title: LocalizedStringResource = "List Rate Alerts"
    static var description = IntentDescription("The configured rate alerts (on device and pushed).")

    init() {}

    func perform() async throws -> IntentResultContainer<String, Never, Never, IntentDialog> {
        let result = try await AppIntentsBridge.shared.listAlerts()
        if let error = result.errorMessage {
            throw IntentFailure(error)
        }
        guard !result.alerts.isEmpty else {
            return .result(value: "No rate alerts configured.", dialog: "No rate alerts configured yet.")
        }
        let lines = result.alerts.map { row in
            let whereFrom = row.source == "SERVER" ? "pushed" : "on device"
            let kind = row.mode == "THRESHOLD" ? "±\(row.thresholdPercent)%" : "every \(row.intervalMinutes / 60)h"
            let state = row.enabled ? "on" : "off"
            return "\(row.base) → \(row.quote) (\(kind), \(whereFrom), \(state))"
        }
        let summary = lines.joined(separator: "\n")
        return .result(
            value: summary,
            dialog: "\(result.alerts.count) alert\(result.alerts.count == 1 ? "" : "s"): \(lines.joined(separator: "; "))"
        )
    }
}

// MARK: - Delete Rate Alert

@available(iOS 16.0, *)
struct DeleteRateAlertIntent: AppIntent {
    static var title: LocalizedStringResource = "Delete Rate Alert"
    static var description = IntentDescription("Removes one configured rate alert.")

    @Parameter(title: "Alert")
    var alert: AlertEntity

    static var parameterSummary: some ParameterSummary {
        Summary("Delete \(\.$alert)")
    }

    init() {}

    func perform() async throws -> IntentResultContainer<Never, Never, Never, IntentDialog> {
        // id is "<SOURCE>:<rowId>"
        let parts = alert.id.split(separator: ":")
        guard parts.count == 2, let rowId = Int64(parts[1]) else {
            throw IntentFailure("Unrecognized alert reference.")
        }
        let source = String(parts[0])
        let result = try await AppIntentsBridge.shared.deleteAlert(alertId: rowId, source: source)
        if let error = result.errorMessage {
            throw IntentFailure(error)
        }
        return .result(dialog: "Deleted the \(alert.base) → \(alert.quote) alert.")
    }
}

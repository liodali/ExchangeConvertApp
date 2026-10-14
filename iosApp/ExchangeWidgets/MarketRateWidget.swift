import WidgetKit
import SwiftUI
import AppIntents

// MARK: - Shared data (App Group, kotlinx-compatible Codable)

let appGroupID = "group.dali.hamza.ExchangeConvertApp"
let snapshotsKey = "widgets.snapshots.json"
let configsKey = "widgets.configs.json"
let apiHost = "https://exchange-api-production-dfba.up.railway.app"

struct WidgetSnapshotDTO: Codable {
    let base: String
    let quote: String
    let rate: Double
    let prevClose: Double?
    let fetchedAt: Double
}

struct WidgetConfigDTO: Codable {
    let id: String
    let base: String
    let quote: String
}

func appGroupDefaults() -> UserDefaults? {
    UserDefaults(suiteName: appGroupID)
}

func readSnapshots() -> [String: WidgetSnapshotDTO] {
    guard let data = appGroupDefaults()?.data(forKey: snapshotsKey) else { return [:] }
    let list = (try? JSONDecoder().decode([WidgetSnapshotDTO].self, from: data)) ?? []
    return Dictionary(uniqueKeysWithValues: list.map { ("\($0.base)/\($0.quote)", $0) })
}

func readConfiguredPairs() -> [String] {
    guard let data = appGroupDefaults()?.data(forKey: configsKey) else { return [] }
    let list = (try? JSONDecoder().decode([WidgetConfigDTO].self, from: data)) ?? []
    return list.map { "\($0.base)/\($0.quote)" }
}

/// Direct fallback when no snapshot exists yet (no access key needed).
func fetchRate(base: String, quote: String) async -> Double? {
    guard var comps = URLComponents(string: apiHost + "/latest") else { return nil }
    comps.queryItems = [
        URLQueryItem(name: "base", value: base),
        URLQueryItem(name: "symbol", value: quote),
    ]
    guard let url = comps.url else { return nil }
    struct Response: Codable { let rates: [String: Double]? }
    guard let (data, _) = try? await URLSession.shared.data(from: url),
          let rates = (try? JSONDecoder().decode(Response.self, from: data))?.rates
    else { return nil }
    return rates[quote]
}

// MARK: - Timeline

struct MarketEntry: TimelineEntry {
    let date: Date
    let base: String
    let quote: String
    let rate: Double?
    let prevClose: Double?
    var isPlaceholder = false

    var deltaPercent: Double? {
        guard let rate, let prevClose, prevClose > 0 else { return nil }
        return (rate - prevClose) / prevClose * 100.0
    }
}

// A real enum (String raw values) — the AppIntents metadata processor
// only accepts declared cases. Curated popular pairs (TND home market).
enum PairOption: String, AppEnum {
    case usdEur = "USD/EUR"
    case eurUsd = "EUR/USD"
    case usdTnd = "USD/TND"
    case eurTnd = "EUR/TND"
    case usdGbp = "USD/GBP"
    case gbpTnd = "GBP/TND"
    case usdJpy = "USD/JPY"
    case usdChf = "USD/CHF"
    case eurGbp = "EUR/GBP"
    case usdXau = "USD/XAU"
    case usdBtc = "USD/BTC"
    case eurSar = "EUR/SAR"

    static let typeDisplayRepresentation = TypeDisplayRepresentation(name: "Market")

    static let caseDisplayRepresentations: [PairOption: DisplayRepresentation] = [
        .usdEur: "USD/EUR", .eurUsd: "EUR/USD", .usdTnd: "USD/TND",
        .eurTnd: "EUR/TND", .usdGbp: "USD/GBP", .gbpTnd: "GBP/TND",
        .usdJpy: "USD/JPY", .usdChf: "USD/CHF", .eurGbp: "EUR/GBP",
        .usdXau: "USD/XAU", .usdBtc: "USD/BTC", .eurSar: "EUR/SAR",
    ]

    static let fallback = PairOption.usdEur

    var base: String { String(rawValue.prefix(3)) }
    var quote: String { String(rawValue.suffix(3)) }
}

struct MarketRateIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Market"
    static var description = IntentDescription("Live exchange rate for one pair.")

    @Parameter(title: "Market")
    var pair: PairOption?

    init() {}

    init(pair: PairOption) {
        self.pair = pair
    }
}

struct MarketRateProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> MarketEntry {
        MarketEntry(date: Date(), base: "USD", quote: "EUR", rate: nil, prevClose: nil, isPlaceholder: true)
    }

    func entry(for configuration: MarketRateIntent) async -> MarketEntry {
        let option = configuration.pair ?? PairOption.fallback
        var snapshot = readSnapshots()["\(option.base)/\(option.quote)"]
        if snapshot == nil {
            // no cached snapshot in the App Group — one direct fetch
            if let rate = await fetchRate(base: option.base, quote: option.quote) {
                snapshot = WidgetSnapshotDTO(
                    base: option.base, quote: option.quote,
                    rate: rate, prevClose: nil, fetchedAt: Date().timeIntervalSince1970 * 1000
                )
            }
        }
        return MarketEntry(
            date: Date(),
            base: option.base,
            quote: option.quote,
            rate: snapshot?.rate,
            prevClose: snapshot?.prevClose
        )
    }

    func snapshot(for configuration: MarketRateIntent, in context: Context) async -> MarketEntry {
        await entry(for: configuration)
    }

    func timeline(for configuration: MarketRateIntent, in context: Context) async -> Timeline<MarketEntry> {
        // hourly entries for the next ~6h — inside the WidgetKit budget
        var entries: [MarketEntry] = []
        for hour in 0..<6 {
            let entry = await entry(for: configuration)
            entries.append(
                MarketEntry(
                    date: Calendar.current.date(byAdding: .hour, value: hour, to: Date())!,
                    base: entry.base, quote: entry.quote,
                    rate: entry.rate, prevClose: entry.prevClose
                )
            )
        }
        return Timeline(entries: entries, policy: .after(Calendar.current.date(byAdding: .hour, value: 6, to: Date())!))
    }
}

// MARK: - Views (prEnT sketch: rounded card, dark surface, palette accents)

struct MarketRateEntryView: View {
    let entry: MarketEntry

    private let surface = Color(red: 42 / 255, green: 42 / 255, blue: 42 / 255)
    private let textPrimary = Color(red: 229 / 255, green: 226 / 255, blue: 225 / 255)
    private let textSecondary = Color(red: 197 / 255, green: 198 / 255, blue: 205 / 255)
    private let positive = Color(red: 78 / 255, green: 222 / 255, blue: 163 / 255)
    private let negative = Color(red: 255 / 255, green: 180 / 255, blue: 171 / 255)

    var body: some View {
        VStack(spacing: 6) {
            Text("\(entry.base)/\(entry.quote)")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(textSecondary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(entry.rate.map { rateText($0) } ?? "…")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(textPrimary)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            if let delta = entry.deltaPercent {
                Text(String(format: "%@ %.2f%%", delta >= 0 ? "▲ +" : "▼ -", abs(delta)))
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(delta >= 0 ? positive : negative)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(
                        (delta >= 0 ? positive : negative).opacity(0.15),
                        in: RoundedRectangle(cornerRadius: 10)
                    )
            } else if entry.isPlaceholder {
                Text("Add from app")
                    .font(.system(size: 9))
                    .foregroundStyle(textSecondary.opacity(0.6))
            }
        }
    }

    private func rateText(_ value: Double) -> String {
        let decimals: Int
        switch value {
        case 100...: decimals = 2
        case 1..<100: decimals = 4
        case 0.01..<1: decimals = 5
        default: decimals = 7
        }
        return String(format: "%.\(decimals)f", value)
    }
}

struct MarketRateWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "MarketRate", intent: MarketRateIntent.self, provider: MarketRateProvider()) { entry in
            MarketRateEntryView(entry: entry)
                .containerBackground(for: .widget) {
                    Color(red: 42 / 255, green: 42 / 255, blue: 42 / 255)
                }
        }
        .configurationDisplayName("Market rate")
        .description("Live exchange rate for one pair.")
        .supportedFamilies([.systemSmall])
    }
}

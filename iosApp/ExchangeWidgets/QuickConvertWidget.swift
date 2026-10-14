import WidgetKit
import SwiftUI
import AppIntents

// Quick Convert (GDiky sketch, systemMedium): pair via the shared AppIntent,
// amount presets as iOS 17 interactive buttons, result from the cached
// snapshot — Glance's preset-chips concept, native on iOS.

struct QuickEntry: TimelineEntry {
    let date: Date
    let base: String
    let quote: String
    let amount: Double
    let rate: Double?
}

struct QuickAmountIntent: AppIntent {
    static var title: LocalizedStringResource = "Amount"
    static var description = IntentDescription("Sets the conversion amount.")

    @Parameter(title: "Pair")
    var pairRaw: String

    @Parameter(title: "Amount")
    var amount: Double

    init() {}

    init(pairRaw: String, amount: Double) {
        self.pairRaw = pairRaw
        self.amount = amount
    }

    func perform() async throws -> some IntentResult {
        if let defaults = appGroupDefaults() {
            defaults.set(amount, forKey: "widgets.ios.quick.amount.\(pairRaw)")
        }
        return .result()
    }
}

func quickAmount(for pairRaw: String) -> Double {
    appGroupDefaults()?.double(forKey: "widgets.ios.quick.amount.\(pairRaw)") ?? 0
}

struct QuickProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> QuickEntry {
        QuickEntry(date: Date(), base: "EUR", quote: "USD", amount: 100, rate: 1.0842)
    }

    private func resolve(_ configuration: MarketRateIntent) async -> QuickEntry {
        let option = configuration.pair ?? PairOption.fallback
        let raw = option.rawValue
        reportActive(pair: raw)
        let stored = quickAmount(for: raw)
        let amount = stored > 0 ? stored : 100
        var snapshot = readSnapshots()[raw]
        if snapshot == nil {
            if let rate = await fetchRate(base: option.base, quote: option.quote) {
                snapshot = WidgetSnapshotDTO(
                    base: option.base, quote: option.quote,
                    rate: rate, prevClose: nil, fetchedAt: 0
                )
            }
        }
        return QuickEntry(
            date: Date(),
            base: option.base,
            quote: option.quote,
            amount: amount,
            rate: snapshot?.rate
        )
    }

    func snapshot(for configuration: MarketRateIntent, in context: Context) async -> QuickEntry {
        await resolve(configuration)
    }

    func timeline(for configuration: MarketRateIntent, in context: Context) async -> Timeline<QuickEntry> {
        let entry = await resolve(configuration)
        let next = Calendar.current.date(byAdding: .hour, value: 1, to: Date())!
        return Timeline(entries: [entry], policy: .after(next))
    }
}

struct QuickConvertEntryView: View {
    let entry: QuickEntry

    private let surface = Color(red: 42 / 255, green: 42 / 255, blue: 42 / 255)
    private let textPrimary = Color(red: 229 / 255, green: 226 / 255, blue: 225 / 255)
    private let textSecondary = Color(red: 197 / 255, green: 198 / 255, blue: 205 / 255)
    private let accent = Color(red: 78 / 255, green: 222 / 255, blue: 163 / 255)
    private let gold = Color(red: 233 / 255, green: 195 / 255, blue: 73 / 255)

    private let presets: [Double] = [1, 10, 100, 1000]

    var body: some View {
        VStack(spacing: 6) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("AMOUNT").font(.system(size: 9, weight: .bold)).foregroundStyle(.gray)
                    HStack(spacing: 6) {
                        Text(amountLabel).font(.system(size: 20, weight: .bold)).foregroundStyle(textPrimary)
                        codeChip(entry.base)
                    }
                    HStack(spacing: 6) {
                        codeChip(entry.base)
                        Text("⇄").font(.system(size: 14, weight: .bold)).foregroundStyle(textSecondary)
                        codeChip(entry.quote)
                    }
                }
                Spacer(minLength: 8)
                VStack(alignment: .trailing, spacing: 4) {
                    Text("CONVERTED").font(.system(size: 9, weight: .bold)).foregroundStyle(.gray)
                    HStack(spacing: 6) {
                        Text(resultText).font(.system(size: 20, weight: .bold)).foregroundStyle(textPrimary)
                        codeChip(entry.quote)
                    }
                    if let rate = entry.rate {
                        Text("cached rate \(String(format: "%.4f", rate))")
                            .font(.system(size: 9)).foregroundStyle(.gray)
                    }
                }
            }
            HStack(spacing: 6) {
                ForEach(presets, id: \.self) { preset in
                    Button(intent: QuickAmountIntent(pairRaw: "\(entry.base)/\(entry.quote)", amount: preset)) {
                        Text(amountLabel(preset))
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(preset == entry.amount ? accent : textSecondary)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(
                                (preset == entry.amount ? accent : Color(red: 53 / 255, green: 53 / 255, blue: 52 / 255))
                                    .opacity(preset == entry.amount ? 0.2 : 1),
                                in: RoundedRectangle(cornerRadius: 10)
                            )
                    }
                    .buttonStyle(.plain)
                }
                Spacer()
                Text("Open app ↗").font(.system(size: 10, weight: .bold)).foregroundStyle(gold)
            }
        }
        .widgetURL(URL(string: "sovereign://history?base=\(entry.base)&quote=\(entry.quote)"))
    }

    private var amountLabel: String { amountLabel(entry.amount) }

    private func amountLabel(_ value: Double) -> String {
        value >= 1000 ? "\(Int(value / 1000))K" : "\(Int(value))"
    }

    private var resultText: String {
        guard let rate = entry.rate else { return "…" }
        let result = entry.amount * rate
        let decimals: Int
        switch result {
        case 100...: decimals = 2
        case 1..<100: decimals = 4
        case 0.01..<1: decimals = 5
        default: decimals = 7
        }
        return String(format: "%.\(decimals)f", result)
    }

    private func codeChip(_ code: String) -> some View {
        Text(code)
            .font(.system(size: 11, weight: .bold))
            .foregroundStyle(textSecondary)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Color(red: 53 / 255, green: 53 / 255, blue: 52 / 255), in: RoundedRectangle(cornerRadius: 8))
    }
}

struct QuickConvertWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "QuickConvert", intent: MarketRateIntent.self, provider: QuickProvider()) { entry in
            QuickConvertEntryView(entry: entry)
                .containerBackground(for: .widget) {
                    Color(red: 42 / 255, green: 42 / 255, blue: 42 / 255)
                }
        }
        .configurationDisplayName("Quick Convert")
        .description("Type-free conversion with amount presets.")
        .supportedFamilies([.systemMedium])
    }
}

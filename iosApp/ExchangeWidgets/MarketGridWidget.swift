import WidgetKit
import SwiftUI

// Markets grid (p7ayhz sketch, systemMedium): zero-config — mirrors the
// Account market preferences exported to the App Group by the app bridge.

struct GridCard: Identifiable {
    let base: String
    let quote: String
    let rate: Double?
    let delta: Double?

    var id: String { "\(base)/\(quote)" }
}

struct GridEntry: TimelineEntry {
    let date: Date
    let cards: [GridCard]
}

struct GridProvider: TimelineProvider {
    func entry() -> GridEntry {
        let snapshots = readSnapshots()
        let pairs = readMarketPairs().prefix(3)
        let cards = pairs.map { raw in
            let parts = raw.split(separator: "/").map(String.init)
            let snap = snapshots[raw]
            var delta: Double?
            if let rate = snap?.rate, let prev = snap?.prevClose, prev > 0 {
                delta = (rate - prev) / prev * 100.0
            }
            return GridCard(
                base: parts.first ?? "", quote: parts.last ?? "",
                rate: snap?.rate, delta: delta
            )
        }
        return GridEntry(date: Date(), cards: Array(cards))
    }

    func placeholder(in context: Context) -> GridEntry { entry() }

    func getSnapshot(in context: Context, completion: @escaping (GridEntry) -> Void) {
        completion(entry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<GridEntry>) -> Void) {
        let next = Calendar.current.date(byAdding: .hour, value: 1, to: Date())!
        completion(Timeline(entries: [entry()], policy: .after(next)))
    }
}

struct MarketGridEntryView: View {
    let entry: GridEntry

    private let textPrimary = Color(red: 229 / 255, green: 226 / 255, blue: 225 / 255)
    private let textSecondary = Color(red: 197 / 255, green: 198 / 255, blue: 205 / 255)
    private let cardSurface = Color(red: 58 / 255, green: 58 / 255, blue: 57 / 255)
    private let positive = Color(red: 78 / 255, green: 222 / 255, blue: 163 / 255)
    private let negative = Color(red: 255 / 255, green: 180 / 255, blue: 171 / 255)
    private let gold = Color(red: 233 / 255, green: 195 / 255, blue: 73 / 255)

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("MARKETS").font(.system(size: 11, weight: .bold)).foregroundStyle(textSecondary)
                Text("your markets").font(.system(size: 8)).foregroundStyle(.gray)
                Spacer()
                Text("Manage").font(.system(size: 10, weight: .bold)).foregroundStyle(gold)
            }
            if entry.cards.count < 2 {
                Spacer()
                VStack(spacing: 4) {
                    Text("Pick 2–3 markets").font(.system(size: 12, weight: .bold)).foregroundStyle(textSecondary)
                    Text("Account → Market Preferences").font(.system(size: 9)).foregroundStyle(.gray)
                }
                Spacer()
            } else {
                // equal share — 2 markets each get the extra space (max 3)
                HStack(spacing: 10) {
                    ForEach(entry.cards.prefix(3).indices, id: \.self) { index in
                        let card = entry.cards[index]
                        VStack(spacing: 6) {
                            Text("\(card.base)/\(card.quote)")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundStyle(textSecondary)
                                .minimumScaleFactor(0.7)
                            Text(card.rate.map { rateText($0) } ?? "…")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(textPrimary)
                                .minimumScaleFactor(0.6)
                            if let delta = card.delta {
                                Text(String(format: "%@ %.2f%%", delta >= 0 ? "▲ +" : "▼ -", abs(delta)))
                                    .font(.system(size: 9, weight: .bold))
                                    .foregroundStyle(delta >= 0 ? positive : negative)
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(
                                        (delta >= 0 ? positive : negative).opacity(0.15),
                                        in: RoundedRectangle(cornerRadius: 8)
                                    )
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(cardSurface, in: RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
        }
        .widgetURL(URL(string: "sovereign://open"))
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

struct MarketGridWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "MarketGrid", provider: GridProvider()) { entry in
            MarketGridEntryView(entry: entry)
                .containerBackground(for: .widget) {
                    Color(red: 42 / 255, green: 42 / 255, blue: 42 / 255)
                }
        }
        .configurationDisplayName("Markets · Grid")
        .description("Up to three markets from your app selection.")
        .supportedFamilies([.systemMedium])
    }
}

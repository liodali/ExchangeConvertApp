import SwiftUI

/// Design system colors matching ui-design.pen
struct DesignColors {
    static let background = Color(hex: "131313")
    static let surface = Color(hex: "1c1b1b")
    static let surfaceVariant = Color(hex: "2a2a2a")
    static let onBackground = Color(hex: "e5e2e1")
    static let onSurface = Color(hex: "c5c6cd")
    static let onSurfaceMuted = Color(hex: "8f9097")
    static let stroke = Color(hex: "44474d")
    static let success = Color(hex: "4edea3")
    static let error = Color(hex: "ffb4ab")
    static let cardBackground = Color(hex: "201f1f")
    static let buttonBackground = Color(hex: "353534")
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

/// RateCard component matching the design from ui-design.pen
/// Displays currency code, name, rate value, and change percentage
struct RateCardView: View {
    let currencyCode: String
    let currencyName: String
    let rateValue: Double
    let changePercent: Double?
    
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                // Currency Info (left side)
                VStack(alignment: .leading, spacing: 4) {
                    Text(currencyCode)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(DesignColors.onBackground)
                    
                    Text(currencyName)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(DesignColors.onSurfaceMuted)
                }
                
                Spacer()
                
                // Rate Value (right side)
                VStack(alignment: .trailing, spacing: 4) {
                    Text(formatRateValue(rateValue))
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(DesignColors.onBackground)
                    
                    if let change = changePercent {
                        Text(formatChangePercent(change))
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(change >= 0 ? DesignColors.success : DesignColors.error)
                    }
                }
            }
            .padding(20)
        }
        .background(DesignColors.cardBackground)
        .cornerRadius(16)
    }
    
    /// Formats the rate value based on magnitude for optimal display
    private func formatRateValue(_ value: Double) -> String {
        switch value {
        case 10000...:
            return String(format: "%.0f", value)
        case 1000..<10000:
            return String(format: "%.2f", value)
        case 1..<1000:
            return String(format: "%.2f", value)
        default:
            return String(format: "%.4f", value)
        }
    }
    
    /// Formats the change percentage with + or - sign
    private func formatChangePercent(_ percent: Double) -> String {
        let sign = percent >= 0 ? "+" : ""
        return String(format: "\(sign)%.2f%%", percent)
    }
}

#Preview {
    RateCardView(
        currencyCode: "EUR",
        currencyName: "Euro",
        rateValue: 0.92,
        changePercent: 0.15
    )
    .padding()
    .background(DesignColors.background)
}

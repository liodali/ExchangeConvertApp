import SwiftUI

/// HistoryGraphSection component showing 30-day price trend
/// Uses mock data as specified in the design
struct HistoryGraphSection: View {
    let baseCurrency: String
    let targetCurrency: String
    
    // Generate mock 30-day data
    private var mockDataPoints: [(Date, Double)] {
        generateMockDataPoints()
    }
    
    var body: some View {
        VStack(spacing: 16) {
            // Header
            VStack(alignment: .leading, spacing: 8) {
                Text("Price History")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(DesignColors.onBackground)
                
                Text("30 day trend (mock data)")
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(DesignColors.onSurfaceMuted)
            }
            
            // Graph Area (placeholder for actual chart implementation)
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(DesignColors.surfaceVariant.opacity(0.25))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(DesignColors.stroke.opacity(0.15), lineWidth: 1)
                    )
                
                VStack(spacing: 4) {
                    Text("Line chart: \(baseCurrency) → \(targetCurrency)")
                        .font(.system(size: 14, weight: .regular))
                        .foregroundColor(DesignColors.onSurfaceMuted)
                    
                    Text("(Mock data for 30 days)")
                        .font(.system(size: 14, weight: .regular))
                        .foregroundColor(DesignColors.onSurfaceMuted)
                }
            }
            .frame(height: 240)
        }
        .padding(24)
        .background(DesignColors.cardBackground)
        .cornerRadius(16)
    }
    
    /// Generates mock data points for 30-day history
    /// Simulates realistic currency rate fluctuations
    private func generateMockDataPoints() -> [(Date, Double)] {
        var dataPoints: [(Date, Double)] = []
        var baseRate = 0.92 // Starting rate for USD/EUR mock
        
        let calendar = Calendar.current
        let now = Date()
        
        for i in stride(from: 29, through: 0, by: -1) {
            if let date = calendar.date(byAdding: .day, value: -i, to: now) {
                // Add some random fluctuation (±2%)
                let fluctuation = (Double.random(in: 0...1) - 0.5) * 0.04
                baseRate = baseRate * (1 + fluctuation)
                dataPoints.append((date, baseRate))
            }
        }
        
        return dataPoints
    }
}

#Preview {
    HistoryGraphSection(baseCurrency: "USD", targetCurrency: "EUR")
        .padding()
        .background(DesignColors.background)
}

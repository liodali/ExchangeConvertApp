import SwiftUI
import SharedKMP

/// QuickExchangeSection component for currency conversion
/// Uses API rates for calculation: result = amount * (rates[toCurrency] / rates[fromCurrency])
struct QuickExchangeSection: View {
    let baseCurrency: String
    let rates: [String: Double]
    let onConvertClick: (Double, String, String) -> Void
    let onFromCurrencyClick: () -> Void
    let onToCurrencyClick: () -> Void
    
    @State private var amount: String = ""
    @State private var fromCurrency: String
    @State private var toCurrency: String = "EUR"
    @State private var result: String = "0.00"
    
    init(
        baseCurrency: String = "USD",
        rates: [String: Double] = [:],
        onConvertClick: @escaping (Double, String, String) -> Void = { _, _, _ in },
        onFromCurrencyClick: @escaping () -> Void = {},
        onToCurrencyClick: @escaping () -> Void = {}
    ) {
        self.baseCurrency = baseCurrency
        self.rates = rates
        self.onConvertClick = onConvertClick
        self.onFromCurrencyClick = onFromCurrencyClick
        self.onToCurrencyClick = onToCurrencyClick
        _fromCurrency = State(initialValue: baseCurrency)
    }
    
    var body: some View {
        VStack(spacing: 20) {
            // Header
            VStack(alignment: .leading, spacing: 8) {
                Text("Quick Exchange")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(DesignColors.onBackground)
                
                Text("Convert currencies using live rates")
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(DesignColors.onSurfaceMuted)
            }
            
            // Amount Input
            VStack(alignment: .leading, spacing: 8) {
                Text("Amount")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(DesignColors.onSurfaceMuted)
                
                TextField("0.00", text: $amount)
                    .keyboardType(.decimalPad)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(DesignColors.onBackground)
                    .padding(16)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(DesignColors.surfaceVariant)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(DesignColors.stroke.opacity(0.15), lineWidth: 1)
                            )
                    )
            }
            
            // Currency Row (From/To)
            HStack(spacing: 12) {
                // From Currency
                CurrencyButton(
                    currency: fromCurrency,
                    onClick: onFromCurrencyClick
                )
                
                // To Currency
                CurrencyButton(
                    currency: toCurrency,
                    onClick: onToCurrencyClick
                )
            }
            
            // Result Display
            HStack {
                Text("Converted Amount")
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(DesignColors.onSurfaceMuted)
                
                Spacer()
                
                Text(result)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(DesignColors.onBackground)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(DesignColors.buttonBackground.opacity(0.2))
            )
            
            // Convert Button
            Button(action: {
                calculateConversion()
                if let amountValue = Double(amount) {
                    onConvertClick(amountValue, fromCurrency, toCurrency)
                }
            }) {
                Text("Convert")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(DesignColors.success)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
            }
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(DesignColors.success.opacity(0.2))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(DesignColors.success)
                    )
            )
        }
        .padding(24)
        .background(DesignColors.cardBackground)
        .cornerRadius(16)
    }
    
    private func calculateConversion() {
        guard let amountValue = Double(amount), amountValue > 0 else {
            result = "0.00"
            return
        }
        
        if !rates.isEmpty {
            // Formula: amount * (rates[toCurrency] / rates[fromCurrency])
            // If fromCurrency is base, use rate directly
            let fromRate = fromCurrency == baseCurrency ? 1.0 : (rates[fromCurrency] ?? 1.0)
            let toRate = toCurrency == baseCurrency ? 1.0 : (rates[toCurrency] ?? 1.0)
            let convertedAmount = amountValue * (toRate / fromRate)
            result = String(format: "%.2f", convertedAmount)
        } else {
            result = "0.00"
        }
    }
}

/// Currency button component for From/To selection
struct CurrencyButton: View {
    let currency: String
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            HStack {
                Text(currency)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(DesignColors.onBackground)
                
                Spacer()
                
                Image(systemName: "chevron.down")
                    .font(.system(size: 10))
                    .foregroundColor(DesignColors.onSurfaceMuted)
            }
            .padding(.horizontal, 16)
            .frame(height: 56)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(DesignColors.surfaceVariant)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(DesignColors.stroke.opacity(0.15), lineWidth: 1)
                    )
            )
        }
    }
}

#Preview {
    QuickExchangeSection(baseCurrency: "USD", rates: ["EUR": 0.92, "GBP": 0.79])
        .padding()
        .background(DesignColors.background)
}

import Foundation

// Re-export the Kotlin Multiplatform framework so the app only needs
// `import SharedKMP`.
//
// The `shared` framework is built by the Gradle task
// `:shared:embedAndSignAppleFrameworkForXcode` and copied into Xcode's
// BUILT_PRODUCTS_DIR by the ExchangeConvertApp scheme pre-action.
@_exported import shared

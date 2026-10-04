// swift-tools-version: 5.9
import PackageDescription

// Local Swift package that exposes the Kotlin Multiplatform `shared` framework
// to the iOS app.
//
// The framework itself is produced by Gradle:
//   :shared:embedAndSignAppleFrameworkForXcode
// which runs as a pre-action of the ExchangeConvertApp scheme before Xcode
// compiles anything. See iosApp/ExchangeConvertApp.xcodeproj/xcshareddata/xcschemes.
let package = Package(
    name: "SharedKMP",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "SharedKMP",
            targets: ["SharedKMP"]
        )
    ],
    targets: [
        .target(
            name: "SharedKMP"
        )
    ]
)

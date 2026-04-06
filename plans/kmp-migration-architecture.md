# KMP Migration Architecture Plan

## Overview

This document outlines the architecture and migration plan for converting the ExchangeCurrencyApp to a Kotlin Multiplatform (KMP) project with iOS support using CocoaPods integration and a hybrid SwiftUI + Compose Multiplatform approach.

## Current Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Android App                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   App       │  │    Core     │  │      Domain         │  │
│  │  (UI/VM)    │──│ (Repository)│──│  (Models/Interfaces)│  │
│  └─────────────┘  └──────┬──────┘  └─────────────────────┘  │
│                          │                                   │
│                   ┌──────▼──────┐                           │
│                   │  Database   │                           │
│                   │   (Room)    │                           │
│                   └─────────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

## Target Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Shared KMP Module                            │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │                      commonMain                                  ││
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  ││
│  │  │    Domain    │  │     Core     │  │   UI (Compose MP)    │  ││
│  │  │   (Models)   │  │  (Repository)│  │   (Screens)          │  ││
│  │  └──────────────┘  └──────┬───────┘  └──────────────────────┘  ││
│  │                           │                                     ││
│  │                    ┌──────▼───────┐                            ││
│  │                    │   Database   │                            ││
│  │                    │  (SQLDelight)│                            ││
│  │                    └──────────────┘                            ││
│  └─────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐ │
│  │   androidMain   │  │    iosMain      │  │   iosX64/iosArm64   │ │
│  │  (Actual impl)  │  │ (Actual impl)   │  │   (CocoaPods)       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
    ┌─────────▼─────────┐          ┌─────────▼─────────┐
    │   Android App     │          │    iOS App        │
    │  ┌──────────────┐ │          │  ┌──────────────┐ │
    │  │  Android UI  │ │          │  │   SwiftUI    │ │
    │  │  (Compose)   │ │          │  │   +          │ │
    │  │  Uses shared │ │          │  │  Compose MP  │ │
    │  │  KMP module  │ │          │  │  (Hybrid)    │ │
    │  └──────────────┘ │          │  └──────────────┘ │
    └───────────────────┘          └───────────────────┘
```

## Module Structure

### 1. Shared Module (New)

The `shared` module will contain all common business logic and will be compiled for both Android and iOS.

**Source Sets:**
- `commonMain` - Common code shared across all platforms
- `androidMain` - Android-specific implementations
- `iosMain` - iOS-specific implementations
- `iosX64` / `iosArm64` - iOS native targets

**Dependencies:**
- Kotlinx Coroutines Core
- Ktor Client (with Darwin engine for iOS, OkHttp for Android)
- Kotlinx Serialization
- SQLDelight (for multiplatform database)
- Compose Multiplatform (for shared UI)

### 2. Database Module Migration (Hybrid Approach)

We will use a **hybrid database approach**: **Room for Android** and **SQLDelight for iOS**.

**Why Hybrid Approach?**
- Keep existing Room code on Android - minimal changes
- SQLDelight for iOS - mature and stable KMP support
- Leverages existing Android instrumented tests
- Avoids Room Multiplatform alpha stage risks

**Architecture:**
```
shared/
├── commonMain/
│   └── database/
│       ├── DatabaseDao.kt (common interface)
│       └── DatabaseEntity.kt (common data class)
├── androidMain/
│   └── database/
│       ├── AppDatabase.kt (Room @Database)
│       ├── RoomDatabaseDao.kt (Room @Dao implementation)
│       └── Entities.kt (Room @Entity)
└── iosMain/
    └── database/
        ├── DatabaseDriverFactory.kt (SQLDelight driver)
        └── SqlDelightDatabaseDao.kt (SQLDelight implementation)
```

**Trade-offs:**
- Need to maintain two database implementations
- Manual schema synchronization between Room and SQLDelight
- expect/actual pattern required for database access layer

### 3. Android App Module

Will be updated to:
- Depend on the `shared` module
- Use shared domain models and repository interfaces
- Keep Android-specific UI (can use Compose Multiplatform components)
- Use Koin for dependency injection (with KMP support)

### 4. iOS App (New)

A new iOS project will be created with:
- Xcode project configuration
- CocoaPods integration via `Podfile`
- SwiftUI as the primary UI framework
- Compose Multiplatform views embedded for specific screens
- Native iOS features (navigation, alerts, etc.)

## Technical Decisions

### 1. CocoaPods vs SPM

**Decision: CocoaPods (for now)**

Reasons:
- More mature KMP integration
- Better documentation and community support
- Easier dependency management for existing iOS projects
- SPM support in KMP is still stabilizing

### 2. Database: Hybrid Approach (Room Android + SQLDelight iOS)

**Decision: Room for Android, SQLDelight for iOS**

Reasons:
- Keep existing Room code on Android - minimal changes required
- SQLDelight has mature and stable iOS support
- Leverages existing Android instrumented tests with Room
- Avoids Room Multiplatform alpha stage risks
- Single common interface with expect/actual pattern

### 3. Networking: Complete Ktor Migration

**Decision: Full Ktor client migration, replacing Retrofit**

Reasons:
- Ktor is already partially used in the project
- Full KMP support with platform-specific engines
- Consistent API across Android and iOS
- Better integration with Kotlin coroutines and flows
- No need for Retrofit + Ktor duplication

### 4. UI Strategy: Hybrid SwiftUI + Compose Multiplatform

**Decision: Compose Multiplatform for specific screens embedded in SwiftUI**

Reasons:
- Leverage existing Compose UI code from Android
- Maintain native iOS look and feel with SwiftUI
- Gradual migration path
- Best of both worlds: native performance + shared UI

### 5. Dependency Injection: Koin Multiplatform

**Decision: Use Koin for KMP**

Reasons:
- Already used in the project
- Koin supports KMP
- Minimal learning curve
- Works with both Android and iOS

## Migration Steps

### Phase 1: KMP Setup
1. Add KMP plugin to root build.gradle.kts
2. Create shared module with KMP configuration
3. Configure CocoaPods plugin
4. Set up source set structure

### Phase 2: Domain Layer Migration
1. Move domain models to shared/commonMain
2. Move repository interfaces to shared/commonMain
3. Set up expect/actual for platform-specific types

### Phase 3: Core Layer Migration
1. Complete migration from Retrofit to Ktor client
2. Move repository implementations to shared/commonMain
3. Set up platform-specific HTTP client engines (OkHttp for Android, Darwin for iOS)

### Phase 4: Database Migration (Hybrid)
1. **Android (Room)**: Keep existing Room database in `androidMain`
2. **iOS (SQLDelight)**:
   - Add SQLDelight Gradle plugin
   - Create `.sq` schema files from existing Room entities
   - Create SQLDelight DAO interfaces
3. Set up expect/actual pattern:
   - Common interface in `commonMain`
   - Room implementation in `androidMain`
   - SQLDelight implementation in `iosMain`

### Phase 5: UI Layer
1. Create Compose Multiplatform screens in shared module
2. Update Android app to use shared screens
3. Create iOS project with SwiftUI
4. Integrate Compose Multiplatform views in SwiftUI

### Phase 6: Integration & Testing
1. Configure Android app dependencies
2. Set up iOS CocoaPods integration
3. Test compilation for both platforms
4. Run end-to-end tests

## File Structure After Migration

```
ExchangeConvertApp/
├── build.gradle.kts (root)
├── settings.gradle.kts
├── gradle.properties
├── shared/
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── commonMain/
│   │   │   ├── kotlin/
│   │   │   │   ├── dali/hamza/shared/
│   │   │   │   │   ├── domain/
│   │   │   │   │   │   ├── models/
│   │   │   │   │   │   └── repository/
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── network/
│   │   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── database/
│   │   │   │   │   └── ui/
│   │   │   │   │       ├── screens/
│   │   │   │   │       ├── components/
│   │   │   │   │       └── theme/
│   │   │   └── resources/
│   │   ├── androidMain/
│   │   │   └── kotlin/
│   │   └── iosMain/
│   │       └── kotlin/
│   └── shared.podspec
├── app/
│   ├── build.gradle.kts
│   └── src/main/...
├── database/ (will be deprecated/migrated)
├── core/ (will be deprecated/migrated)
├── domain/ (will be deprecated/migrated)
└── iosApp/
    ├── ExchangeConvertApp.xcodeproj/
    ├── ExchangeConvertApp/
    │   ├── AppDelegate.swift
    │   ├── SceneDelegate.swift
    │   ├── ContentView.swift
    │   └── ...
    └── Podfile
```

## Dependencies

### Shared Module (KMP)
```kotlin
// Core
kotlin("multiplatform")
kotlin("native.cocoapods")

// Compose Multiplatform
compose.compiler
org.jetbrains.compose

// Coroutines
kotlinx-coroutines-core

// Networking (complete Ktor migration - no Retrofit)
ktor-client-core
ktor-client-content-negotiation
ktor-serialization-kotlinx-json
ktor-client-okhttp (android)
ktor-client-darwin (ios)

// Serialization
kotlinx-serialization-json

// Database - Hybrid Approach (Room Android + SQLDelight iOS)
// Common (interface only - no dependency)

// Android (Room - keep existing)
androidx.room:room-runtime
androidx.room:room-ktx
androidx.room:room-paging
ksp (for Room compiler)

// iOS (SQLDelight)
com.squareup.sqldelight:runtime
com.squareup.sqldelight:coroutines-extensions
com.squareup.sqldelight:native-driver

// DI
io.insert-koin:koin-core

// Compose UI
org.jetbrains.compose.ui
org.jetbrains.compose.foundation
org.jetbrains.compose.material3
```

### Android App
```kotlin
// KMP shared module
implementation(project(":shared"))

// Existing dependencies (updated as needed)
```

### iOS App
```ruby
# Podfile
platform :ios, '15.0'

target 'ExchangeConvertApp' do
  use_frameworks!
  
  pod 'shared', :path => '../shared'
end
```

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Complete Retrofit to Ktor migration | Medium | Thorough testing, gradual endpoint migration |
| Hybrid database (Room + SQLDelight) complexity | Medium | Clear expect/actual interface, careful schema synchronization |
| Manual schema synchronization between Room and SQLDelight | Medium | Document mapping process, keep schemas in sync |
| Compose Multiplatform iOS limitations | Low | Use hybrid approach with SwiftUI |
| CocoaPods integration issues | Low | Follow official KMP documentation |
| Build time increase | Medium | Optimize Gradle configuration |
| Team learning curve | Medium | Documentation and training |

## Next Steps

1. Review and approve this architecture plan
2. Switch to Code mode to begin implementation
3. Start with Phase 1: KMP Setup
4. Iteratively test and validate each phase

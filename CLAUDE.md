# Snowplow Android Tracker - CLAUDE.md

## Project Overview

The Snowplow Android Tracker is an analytics SDK for collecting behavioral event data from Android applications and sending it to Snowplow collectors. It provides comprehensive event tracking capabilities including ecommerce, media playback, screen views, and custom events. The tracker supports multiple concurrent tracker instances, remote configuration, and WebView integration.

## Development Commands

```bash
# Build the project
./gradlew build

# Run unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Generate API documentation
./gradlew dokkaHtml

# Clean build artifacts
./gradlew clean

# Publish to Maven repository
./gradlew publishToMavenLocal
```

## Architecture

The tracker follows a layered architecture with clear separation of concerns:

1. **Public API Layer** (`com.snowplowanalytics.snowplow`): User-facing API with Snowplow singleton and configuration classes
2. **Core Layer** (`com.snowplowanalytics.core`): Internal implementation including tracker, emitter, and state management
3. **Event Layer**: Event types and payload construction
4. **Network Layer**: HTTP communication with collectors
5. **Storage Layer**: SQLite-based event persistence

### Key Components

- **Tracker**: Core tracking engine managing event creation and processing
- **Emitter**: Batches and sends events to the collector
- **Session**: Manages user session state and lifecycle
- **StateManager**: Coordinates state machines for various tracking features
- **EventStore**: SQLite persistence for offline event storage

## Core Architectural Principles

### 1. Namespace-based Multi-Tracker Support
```kotlin
// ✅ Create trackers with unique namespaces
val tracker1 = Snowplow.createTracker(context, "ecommerce", network1)
val tracker2 = Snowplow.createTracker(context, "analytics", network2)
// ❌ Don't reuse namespaces without removing first
val duplicate = Snowplow.createTracker(context, "ecommerce", network3) // Will reset existing
```

### 2. Configuration Immutability
```kotlin
// ✅ Configure at creation time
val tracker = Snowplow.createTracker(context, namespace, network, 
    TrackerConfiguration(appId).apply { base64 = false })
// ❌ Don't modify configuration after creation
trackerConfig.base64 = true // Has no effect after tracker creation
```

### 3. Event Builder Pattern
```kotlin
// ✅ Use builder pattern for events
val event = Structured("category", "action")
    .label("label")
    .property("property")
    .value(10.0)
    .entities(listOf(customContext))
// ❌ Don't modify events after tracking
tracker.track(event)
event.label = "newLabel" // Too late, event already sent
```

### 4. Asynchronous Event Processing
```kotlin
// ✅ Events are queued and sent asynchronously
tracker.track(event) // Returns immediately
// ❌ Don't expect synchronous sending
tracker.track(event)
// Event may not be sent yet at this point
```

## Layer Organization & Responsibilities

### Public API Layer (`snowplow` package)
- User-facing configuration classes
- Event type definitions
- Controller interfaces for runtime management
- Snowplow singleton for tracker management

### Core Implementation Layer (`core` package)
- Internal tracker implementation
- Emitter and network management
- State machine implementations
- Platform-specific utilities

### Event Processing Pipeline
1. Event Creation → 2. Enrichment → 3. Validation → 4. Storage → 5. Batching → 6. Network Transmission

## Critical Import Patterns

### Configuration Imports
```kotlin
// ✅ Correct configuration imports
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.*
import com.snowplowanalytics.snowplow.network.HttpMethod
// ❌ Don't import internal core classes
import com.snowplowanalytics.core.tracker.Tracker // Internal only
```

### Event Imports
```kotlin
// ✅ Correct event imports
import com.snowplowanalytics.snowplow.event.*
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson
// ❌ Don't mix with core event classes
import com.snowplowanalytics.core.event.* // Internal processing
```

## Essential Library Patterns

### 1. Tracker Initialization Pattern
```kotlin
// ✅ Standard initialization
val networkConfig = NetworkConfiguration("https://collector.example.com")
val trackerConfig = TrackerConfiguration("app-id")
    .sessionContext(true)
    .platformContext(true)
val tracker = Snowplow.createTracker(context, "namespace", networkConfig, trackerConfig)
```

### 2. Event Tracking Pattern
```kotlin
// ✅ Track with entities
val event = ScreenView("home_screen")
    .entities(listOf(
        SelfDescribingJson("iglu:com.example/context/jsonschema/1-0-0", mapOf("key" to "value"))
    ))
tracker.track(event)
```

### 3. Remote Configuration Pattern
```kotlin
// ✅ Setup with remote config
Snowplow.setup(context, 
    RemoteConfiguration("https://config.example.com/config.json", HttpMethod.GET),
    listOf(defaultBundle)
) { result ->
    val namespaces = result.first
    val configState = result.second
}
```

### 4. Subject Configuration Pattern
```kotlin
// ✅ Configure subject (user) properties
tracker.subject.apply {
    userId = "user123"
    screenResolution = Size(1920, 1080)
    language = "en-US"
}
```

## Model Organization Pattern

### Event Hierarchy
```kotlin
// Base event classes
abstract class AbstractEvent : Event
abstract class AbstractPrimitive : AbstractEvent() // For structured events
abstract class AbstractSelfDescribing : AbstractEvent() // For self-describing events

// Concrete events extend appropriate base
class ScreenView : AbstractSelfDescribing()
class Structured : AbstractPrimitive()
```

### Entity Pattern
```kotlin
// ✅ Create entities with schema and data
val entity = SelfDescribingJson(
    schema = "iglu:com.example/entity/jsonschema/1-0-0",
    data = mapOf("property" to "value")
)
// ❌ Don't use raw JSON strings
val entity = SelfDescribingJson(jsonString) // Use structured data
```

## Common Pitfalls & Solutions

### 1. Event Store Zombie Events
```kotlin
// ❌ Problem: Changing namespace leaves orphaned events
val tracker1 = createTracker(context, "oldNamespace", network)
// Later...
val tracker2 = createTracker(context, "newNamespace", network)
// Old events stuck in database

// ✅ Solution: Clean up orphaned events
SQLiteEventStore.removeUnsentEventsExceptForNamespaces(listOf("newNamespace"))
```

### 2. Configuration Timing
```kotlin
// ❌ Problem: Configuring after creation
val tracker = Snowplow.createTracker(context, namespace, network)
tracker.emitter.bufferOption = BufferOption.LargeGroup // No effect

// ✅ Solution: Configure during creation
val emitterConfig = EmitterConfiguration()
    .bufferOption(BufferOption.LargeGroup)
val tracker = Snowplow.createTracker(context, namespace, network, emitterConfig)
```

### 3. WebView Integration
```kotlin
// ❌ Problem: Forgetting to subscribe WebView
webView.loadUrl("https://example.com")

// ✅ Solution: Subscribe before loading
Snowplow.subscribeToWebViewEvents(webView)
webView.loadUrl("https://example.com")
```

### 4. Session Management
```kotlin
// ❌ Problem: Not handling background/foreground transitions
// Sessions may timeout incorrectly

// ✅ Solution: Use lifecycle-aware session configuration
val sessionConfig = SessionConfiguration(
    TimeMeasure(30, TimeUnit.MINUTES),  // Foreground timeout
    TimeMeasure(30, TimeUnit.MINUTES)   // Background timeout
)
```

## File Structure Template

```
snowplow-android-tracker/
├── snowplow-tracker/               # Main library module
│   ├── src/main/java/
│   │   ├── com/snowplowanalytics/
│   │   │   ├── snowplow/          # Public API
│   │   │   │   ├── configuration/ # Configuration classes
│   │   │   │   ├── controller/    # Runtime controllers
│   │   │   │   ├── event/        # Event types
│   │   │   │   ├── ecommerce/    # Ecommerce tracking
│   │   │   │   ├── media/        # Media tracking
│   │   │   │   └── Snowplow.kt   # Main entry point
│   │   │   └── core/              # Internal implementation
│   │   │       ├── emitter/      # Event batching/sending
│   │   │       ├── tracker/      # Core tracker logic
│   │   │       └── statemachine/ # State management
│   └── src/androidTest/          # Instrumentation tests
├── snowplow-demo-kotlin/          # Kotlin demo app
├── snowplow-demo-java/            # Java demo app
├── snowplow-demo-compose/         # Compose demo app
└── build.gradle                   # Root build configuration
```

## Quick Reference

### Tracker Initialization Checklist
- [ ] Create NetworkConfiguration with collector URL
- [ ] Create TrackerConfiguration with app ID
- [ ] Add optional configurations (Session, Subject, GlobalContexts)
- [ ] Call Snowplow.createTracker with unique namespace
- [ ] Store TrackerController reference for runtime control

### Event Tracking Checklist
- [ ] Choose appropriate event type (ScreenView, Structured, SelfDescribing, etc.)
- [ ] Set required event properties
- [ ] Add custom entities if needed
- [ ] Call tracker.track(event)
- [ ] Handle any tracking failures via EmitterController

### Common Configuration Objects
- `NetworkConfiguration`: Collector endpoint and HTTP method
- `TrackerConfiguration`: Core tracker settings
- `SessionConfiguration`: Session timeout and lifecycle
- `EmitterConfiguration`: Batching and retry settings
- `SubjectConfiguration`: User properties
- `GlobalContextsConfiguration`: Automatic context addition
- `GdprConfiguration`: GDPR context management

## Contributing to CLAUDE.md

When adding or updating content in this document, please follow these guidelines:

### File Size Limit
- **CLAUDE.md must not exceed 40KB** (currently ~19KB)
- Check file size after updates: `wc -c CLAUDE.md`
- Remove outdated content if approaching the limit

### Code Examples
- Keep all code examples **4 lines or fewer**
- Focus on the essential pattern, not complete implementations
- Use `// ❌` and `// ✅` to clearly show wrong vs right approaches

### Content Organization
- Add new patterns to existing sections when possible
- Create new sections sparingly to maintain structure
- Update the architectural principles section for major changes
- Ensure examples follow current codebase conventions

### Quality Standards
- Test any new patterns in actual code before documenting
- Verify imports and syntax are correct for the codebase
- Keep language concise and actionable
- Focus on "what" and "how", minimize "why" explanations

### Multiple CLAUDE.md Files
- **Directory-specific CLAUDE.md files** can be created for specialized modules
- Follow the same structure and guidelines as this root CLAUDE.md
- Keep them focused on directory-specific patterns and conventions
- Maximum 20KB per directory-specific CLAUDE.md file

### Instructions for LLMs
When editing files in this repository, **always check for CLAUDE.md guidance**:

1. **Look for CLAUDE.md in the same directory** as the file being edited
2. **If not found, check parent directories** recursively up to project root
3. **Follow the patterns and conventions** described in the applicable CLAUDE.md
4. **Prioritize directory-specific guidance** over root-level guidance when conflicts exist
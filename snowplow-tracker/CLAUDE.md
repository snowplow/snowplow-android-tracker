# Snowplow Tracker Module - CLAUDE.md

## Module Overview

The `snowplow-tracker` module is the core Android library that implements the Snowplow event tracking SDK. It provides the complete implementation of event tracking, network communication, state management, and data persistence. This module is published as a standalone AAR library that applications can include as a dependency.

## Module Structure

### Package Organization

```
com.snowplowanalytics/
├── snowplow/           # Public API surface
│   ├── configuration/  # User-facing configuration classes
│   ├── controller/     # Runtime control interfaces
│   ├── event/         # Event type definitions
│   ├── payload/       # Payload construction
│   └── Snowplow.kt    # Main entry point
└── core/              # Internal implementation (not public API)
    ├── emitter/       # Event batching and sending
    ├── tracker/       # Core tracking logic
    ├── session/       # Session management
    └── statemachine/  # State coordination
```

## Testing Patterns

### Instrumentation Test Structure
```kotlin
// ✅ Use test utilities and mocks
class TrackerTest {
    @Before fun setUp() {
        TestUtils.createSessionSharedPreferences(context, namespace)
        tracker = createTestTracker()
    }
    @Test fun testEventTracking() {
        val mockEventStore = MockEventStore()
        // Test implementation
    }
}
```

### Mock Implementations
```kotlin
// ✅ Create focused mock implementations
class MockNetworkConnection : NetworkConnection {
    var sendingCount = AtomicInteger(0)
    override fun sendRequests(requests: List<Request>) {
        sendingCount.addAndGet(requests.size)
    }
}
```

## Event Implementation Patterns

### Creating New Event Types
```kotlin
// ✅ Extend appropriate base class
class CustomEvent(val customProperty: String) : AbstractSelfDescribing() {
    override val schema = "iglu:com.example/custom/jsonschema/1-0-0"
    override val dataPayload: Map<String, Any?>
        get() = mapOf("custom_property" to customProperty)
}
// ❌ Don't implement Event interface directly
class BadEvent : Event { } // Missing required functionality
```

### Event Processing Lifecycle
```kotlin
// ✅ Implement lifecycle methods for stateful events
override fun beginProcessing(tracker: Tracker) {
    // Add processing-time data
    isProcessing = true
}
override fun endProcessing(tracker: Tracker) {
    // Clean up after processing
    isProcessing = false
}
```

## State Machine Patterns

### Implementing State Machines
```kotlin
// ✅ Extend StateMachineInterface
class CustomStateMachine : StateMachineInterface {
    override val identifier = "CustomStateMachine"
    override val subscribedEventSchemasForTransitions = listOf("iglu:com.snowplowanalytics.*/event/*/1-*-*")
    
    override fun transition(event: Event, state: State?): State? {
        // Implement state transition logic
        return CustomState()
    }
}
```

### State Management
```kotlin
// ✅ Use StateManager for coordination
stateManager.addOrReplaceStateMachine(CustomStateMachine())
val state = stateManager.trackerState.getState(CustomStateMachine.ID)
```

## Network Layer Patterns

### Custom Network Implementation
```kotlin
// ✅ Implement NetworkConnection interface
class CustomNetworkConnection : NetworkConnection {
    override fun sendRequests(requests: List<Request>) {
        requests.forEach { request ->
            // Custom sending logic
            val result = sendRequest(request)
            request.callback?.onComplete(result)
        }
    }
}
```

### Request Handling
```kotlin
// ✅ Use RequestCallback for async results
val request = Request(payload, emitterUri).apply {
    callback = object : RequestCallback {
        override fun onComplete(result: RequestResult) {
            if (result.isSuccessful) handleSuccess()
            else handleFailure()
        }
    }
}
```

## Storage Layer Patterns

### Event Store Implementation
```kotlin
// ✅ SQLiteEventStore usage
val eventStore = SQLiteEventStore(context, namespace).apply {
    // Events are automatically persisted
}
// ❌ Don't access database directly
val db = SQLiteDatabase.openDatabase(...) // Use EventStore abstraction
```

### Event Persistence
```kotlin
// ✅ Events are persisted before sending
tracker.track(event) // Saved to EventStore
// Network failure doesn't lose events
emitter.flush() // Retry from EventStore
```

## Platform Context Management

### Custom Platform Context Properties
```kotlin
// ✅ Use PlatformContextRetriever for custom values
val retriever = PlatformContextRetriever().apply {
    appleIdfa = { "custom-idfa-value" }
    deviceManufacturer = { "Custom Manufacturer" }
}
val tracker = Tracker(emitter, namespace, appId, 
    platformContextRetriever = retriever, context = context)
```

### Platform Context Properties Selection
```kotlin
// ✅ Select specific properties to track
val properties = listOf(
    PlatformContextProperty.DEVICE_MODEL,
    PlatformContextProperty.OS_VERSION,
    PlatformContextProperty.APP_VERSION
)
```

## Emitter Configuration Patterns

### Buffer Management
```kotlin
// ✅ Configure buffer options
val emitterConfig = EmitterConfiguration()
    .bufferOption(BufferOption.LargeGroup) // 25 events
    .emitRange(10) // Send 10 events at a time
    .byteLimitPost(40_000) // 40KB POST limit
```

### Custom Emitter Implementation
```kotlin
// ✅ Extend Emitter class
class CustomEmitter(context: Context, namespace: String) : 
    Emitter(networkConnection, context, namespace) {
    override fun flush() {
        // Custom flush logic
        super.flush()
    }
}
```

## Common Implementation Pitfalls

### 1. Direct Core Package Access
```kotlin
// ❌ Problem: Using internal classes
import com.snowplowanalytics.core.tracker.Tracker
val tracker = Tracker(...) // Internal API

// ✅ Solution: Use public API
import com.snowplowanalytics.snowplow.Snowplow
val tracker = Snowplow.createTracker(...)
```

### 2. Synchronous Event Sending
```kotlin
// ❌ Problem: Expecting immediate send
tracker.track(event)
assert(mockServer.requestCount == 1) // May fail

// ✅ Solution: Wait for async processing
tracker.track(event)
tracker.emitter.flush()
Thread.sleep(100) // Or use CountDownLatch
```

### 3. State Machine Conflicts
```kotlin
// ❌ Problem: Overlapping event schemas
stateMachine1.subscribedEventSchemasForTransitions = listOf("*")
stateMachine2.subscribedEventSchemasForTransitions = listOf("*")

// ✅ Solution: Use specific schemas
stateMachine1.subscribedEventSchemasForTransitions = 
    listOf("iglu:com.example/specific/*/1-*-*")
```

## Build Configuration

### Gradle Dependencies
```kotlin
// ✅ Module dependencies
dependencies {
    implementation 'androidx.annotation:annotation:1.6.0'
    implementation 'com.squareup.okhttp3:okhttp:4.10.0'
    
    // Testing
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'com.squareup.okhttp3:mockwebserver:4.7.2'
}
```

### ProGuard Rules
```
# ✅ Keep public API classes
-keep class com.snowplowanalytics.snowplow.** { *; }
# ❌ Don't keep internal core classes
-keep class com.snowplowanalytics.core.** { *; } # Too broad
```

## Module-Specific Conventions

### Kotlin-Java Interoperability
```kotlin
// ✅ Use @JvmStatic for Java compatibility
object Snowplow {
    @JvmStatic
    fun createTracker(...): TrackerController { }
}

// ✅ Use @JvmOverloads for optional parameters
@JvmOverloads
fun createTracker(
    context: Context,
    namespace: String,
    endpoint: String,
    method: HttpMethod = HttpMethod.POST
)
```

### Thread Safety
```kotlin
// ✅ Use synchronized for shared state
@Synchronized
private fun registerInstance(serviceProvider: ServiceProvider) { }

// ✅ Use atomic operations
private val _dataCollection = AtomicBoolean(true)
```

## Quick Reference - Module Implementation

### Adding New Event Type Checklist
- [ ] Create event class extending AbstractEvent/AbstractSelfDescribing/AbstractPrimitive
- [ ] Define schema (for self-describing events)
- [ ] Implement dataPayload property
- [ ] Add builder methods for fluent API
- [ ] Create corresponding test in androidTest

### Adding New Configuration Checklist
- [ ] Create configuration class implementing Configuration
- [ ] Add configuration interface in core package
- [ ] Implement controller in core package
- [ ] Wire up in ServiceProvider
- [ ] Add to Snowplow.createTracker method

### Testing New Features Checklist
- [ ] Create instrumentation test in androidTest
- [ ] Use MockEventStore for event verification
- [ ] Use MockNetworkConnection for network testing
- [ ] Test configuration changes
- [ ] Verify thread safety if applicable

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
# Snowplow Kotlin Demo App - CLAUDE.md

## Demo App Overview

The Kotlin demo app showcases best practices for integrating the Snowplow Android Tracker in a Kotlin application. It demonstrates event tracking, media tracking, configuration management, and various tracking scenarios using idiomatic Kotlin patterns.

## Key Implementation Patterns

### Tracker Initialization
```kotlin
// ✅ Initialize tracker with Kotlin DSL style
private fun setupTracker() {
    val networkConfig = NetworkConfiguration(collectorEndpoint, HttpMethod.POST)
    val trackerConfig = TrackerConfiguration(appId).apply {
        base64Encoding = false
        sessionContext = true
        platformContext = true
    }
    tracker = Snowplow.createTracker(this, namespace, networkConfig, trackerConfig)
}
```

### Event Tracking Patterns
```kotlin
// ✅ Use Kotlin's apply for fluent configuration
fun trackScreenView(name: String) {
    tracker.track(ScreenView(name).apply {
        entities = mutableListOf(createCustomContext())
        trueTimestamp = System.currentTimeMillis()
    })
}
```

### Extension Functions
```kotlin
// ✅ Create extension functions for common patterns
fun TrackerController.trackUserAction(action: String, category: String) {
    track(Structured(category, action).apply {
        label = "user-interaction"
        value = 1.0
    })
}
```

### Coroutine Integration
```kotlin
// ✅ Use coroutines for async operations
lifecycleScope.launch {
    withContext(Dispatchers.IO) {
        tracker.track(event)
    }
    withContext(Dispatchers.Main) {
        updateUI()
    }
}
```

## Media Tracking Implementation

### Video Player Integration
```kotlin
// ✅ Implement media tracking controller
class VideoViewController(private val videoView: VideoView) {
    private lateinit var mediaTracking: MediaTracking
    
    fun startTracking(tracker: TrackerController) {
        val config = MediaTrackingConfiguration(
            id = "video-${System.currentTimeMillis()}",
            player = MediaPlayerEntity("android-videoplayer")
        ).apply {
            boundaries = listOf(10, 25, 50, 75)
            captureEvents = listOf(MediaEvent.PLAY, MediaEvent.PAUSE)
        }
        mediaTracking = tracker.media.startMediaTracking(config)
    }
}
```

### Media Event Handling
```kotlin
// ✅ Track media events with player state
videoView.setOnPreparedListener { player ->
    mediaTracking.track(MediaReadyEvent())
    mediaTracking.update(player = MediaPlayerEntity(
        duration = player.duration.toDouble() / 1000
    ))
}
```

## Activity Patterns

### Base Activity Pattern
```kotlin
// ✅ Create base activity for common tracking
abstract class BaseTrackingActivity : AppCompatActivity() {
    protected val tracker: TrackerController by lazy {
        Snowplow.defaultTracker ?: throw IllegalStateException("Tracker not initialized")
    }
    
    override fun onResume() {
        super.onResume()
        trackScreenView()
    }
    
    abstract fun trackScreenView()
}
```

### Lifecycle-aware Tracking
```kotlin
// ✅ Use lifecycle observers
class ScreenTrackingObserver(private val tracker: TrackerController) : 
    DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        tracker.track(Foreground())
    }
    override fun onStop(owner: LifecycleOwner) {
        tracker.track(Background())
    }
}
```

## Configuration Management

### BuildConfig Usage
```kotlin
// ✅ Use BuildConfig for environment-specific values
object TrackerConfig {
    val COLLECTOR_URL = if (BuildConfig.DEBUG) {
        "https://staging-collector.example.com"
    } else {
        "https://collector.example.com"
    }
}
```

### Shared Preferences Integration
```kotlin
// ✅ Store user preferences
class TrackingPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("tracking", Context.MODE_PRIVATE)
    
    var isTrackingEnabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) = prefs.edit().putBoolean("enabled", value).apply()
}
```

## UI Integration Patterns

### Click Tracking
```kotlin
// ✅ Track UI interactions
binding.purchaseButton.setOnClickListener {
    tracker.track(Structured("ui", "click").apply {
        label = "purchase_button"
        property = productId
        value = productPrice
    })
    processPurchase()
}
```

### Form Tracking
```kotlin
// ✅ Track form submissions
fun trackFormSubmission(formData: Map<String, String>) {
    val entity = SelfDescribingJson(
        "iglu:com.example/form_submission/jsonschema/1-0-0",
        formData
    )
    tracker.track(SelfDescribing(entity))
}
```

## Testing Patterns

### Mock Tracker for Testing
```kotlin
// ✅ Create test doubles
class MockTrackerController : TrackerController {
    val trackedEvents = mutableListOf<Event>()
    
    override fun track(event: Event): UUID? {
        trackedEvents.add(event)
        return UUID.randomUUID()
    }
}
```

### UI Testing with Tracking
```kotlin
// ✅ Verify tracking in UI tests
@Test
fun testButtonClickTracking() {
    onView(withId(R.id.button)).perform(click())
    
    verify(mockTracker).track(argThat { event ->
        event is Structured && event.action == "click"
    })
}
```

## Common Demo Pitfalls

### 1. Hardcoded Configuration
```kotlin
// ❌ Problem: Hardcoded values
val tracker = Snowplow.createTracker(this, "demo", "http://localhost:9090")

// ✅ Solution: Use configuration
val tracker = Snowplow.createTracker(this, BuildConfig.NAMESPACE, BuildConfig.COLLECTOR_URL)
```

### 2. Missing Error Handling
```kotlin
// ❌ Problem: No error handling
tracker.track(event)

// ✅ Solution: Handle potential failures
try {
    tracker.track(event)
} catch (e: Exception) {
    Log.e(TAG, "Failed to track event", e)
}
```

### 3. Memory Leaks
```kotlin
// ❌ Problem: Holding activity reference
class EventTracker(private val activity: Activity) {
    fun track() { /* ... */ }
}

// ✅ Solution: Use application context
class EventTracker(private val context: Context) {
    private val appContext = context.applicationContext
}
```

## Demo-Specific Features

### Debug Menu
```kotlin
// ✅ Provide debug controls
class DebugMenuActivity : AppCompatActivity() {
    fun setupDebugControls() {
        binding.flushEvents.setOnClickListener {
            tracker.emitter.flush()
        }
        binding.pauseTracking.setOnClickListener {
            tracker.pause()
        }
    }
}
```

### Event Inspection
```kotlin
// ✅ Log events in debug mode
if (BuildConfig.DEBUG) {
    tracker.emitter.requestCallback = { request ->
        Log.d(TAG, "Sending events: ${request.payload}")
    }
}
```

## Quick Reference - Demo Implementation

### Setting Up Demo Checklist
- [ ] Configure collector endpoint in gradle.properties
- [ ] Set up namespace and app ID
- [ ] Initialize tracker in Application class
- [ ] Add network security config for local testing
- [ ] Configure ProGuard rules if using R8

### Adding Demo Features Checklist
- [ ] Create dedicated activity for feature
- [ ] Implement tracking for all user interactions
- [ ] Add UI to display tracking status
- [ ] Include debug controls for testing
- [ ] Document expected tracking behavior

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
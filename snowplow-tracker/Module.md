# Module snowplow-android-tracker

The [Snowplow Android Tracker](https://github.com/snowplow/snowplow-android-tracker) allows you 
to add analytics to your mobile apps when using a 
[Snowplow](https://github.com/snowplow/snowplow) pipeline.

With this tracker you can collect event data from your applications, games or frameworks.

# Package com.snowplowanalytics.core
Internal use. The main package for the tracker core classes.

# Package com.snowplowanalytics.core.constants
Internal use. Constants used in constructing event requests.

# Package com.snowplowanalytics.core.emitter
Internal use. Classes involved in buffering and sending events.

# Package com.snowplowanalytics.core.emitter.storage
Internal use. Classes for event storage.

# Package com.snowplowanalytics.core.gdpr
Internal use. Manages the GDPR context entity.

# Package com.snowplowanalytics.core.globalcontexts
Internal use. Manages configured GlobalContexts.

# Package com.snowplowanalytics.core.remoteconfiguration
Internal use. Classes to manage remote configuration.

# Package com.snowplowanalytics.core.session
Internal use. Session management.

# Package com.snowplowanalytics.core.statemachine
Internal use. StateMachines are used for managing various states, as well as custom provided plugins.

# Package com.snowplowanalytics.core.tracker
Internal use. Classes for tracking events plus associated data.

# Package com.snowplowanalytics.core.utils
Internal use. Core utils.

# Package com.snowplowanalytics.snowplow
The main package for the published tracker API.

# Package com.snowplowanalytics.snowplow.configuration
Tracker configuration options.

# Package com.snowplowanalytics.snowplow.controller
Interfaces for the various internal parts of the tracker.

# Package com.snowplowanalytics.snowplow.emitter
Interface for the EventStore, plus other published classes to do with event sending.

# Package com.snowplowanalytics.snowplow.entity
Classes for automatically added entities.

# Package com.snowplowanalytics.snowplow.event
Different types of events that can be tracked with Snowplow.

# Package com.snowplowanalytics.snowplow.globalcontexts
Classes for configuring GlobalContexts.

# Package com.snowplowanalytics.snowplow.network
Configure the network connection between tracker and event collector.

# Package com.snowplowanalytics.snowplow.payload
Classes to help construct the event.

# Package com.snowplowanalytics.snowplow.tracker
Supplementary classes for tracker configuration.

# Package com.snowplowanalytics.snowplow.util
Published utils classes.

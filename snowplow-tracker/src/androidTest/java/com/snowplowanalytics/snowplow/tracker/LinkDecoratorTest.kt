package com.snowplowanalytics.snowplow.tracker


import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.utils.Util.urlSafeBase64Encode
import com.snowplowanalytics.snowplow.Snowplow
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration
import com.snowplowanalytics.snowplow.configuration.SessionConfiguration
import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration
import com.snowplowanalytics.snowplow.controller.SessionController
import com.snowplowanalytics.snowplow.controller.TrackerController
import com.snowplowanalytics.snowplow.event.ScreenView
import com.snowplowanalytics.snowplow.network.HttpMethod
import com.snowplowanalytics.snowplow.util.TimeMeasure
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class LinkDecoratorTest {
    private lateinit var tracker: TrackerController
    private lateinit var session: SessionController
    private lateinit var userId: String
    private lateinit var appId: String
    private val subjectUserId = "subjectUserId"
    private val subjectUserIdEncoded = urlSafeBase64Encode(subjectUserId)
    private val testLink = Uri.parse("http://example.com")
    private val epoch = "\\d{13}"

    private fun matches(pattern: String, result: Uri) {
        val regex = Regex("^${pattern.replace(".", "\\.").replace("?", "\\?")}$")
        Assert.assertTrue(
            "$result\ndoes not match expected:  $pattern", regex.matches(result.toString())
        )
    }


    @Before
    fun before() {
        tracker = getTracker()
        session = tracker.session!!
        userId = session.userId
        appId = urlSafeBase64Encode(tracker.appId)
    }

    @Test
    fun testWithoutSession() {
        val tracker = getTrackerNoSession()
        val result = tracker.decorateLink(testLink)
        Assert.assertEquals(null, result)
    }

    @Test
    fun testDecorateUriWithExistingSpParam() {
        tracker.track(ScreenView("test"))

        val pattern = "http://example.com?_sp=$userId.$epoch.${session.sessionId}..$appId"
        val result =
            tracker.decorateLink(testLink.buildUpon().appendQueryParameter("_sp", "test").build())

        matches(pattern, result!!)
    }

    @Test
    fun testDecorateUriWithOtherParam() {
        tracker.track(ScreenView("test"))

        val pattern = "http://example.com?a=b&_sp=$userId.$epoch.${session.sessionId}..$appId$"
        val result =
            tracker.decorateLink(testLink.buildUpon().appendQueryParameter("a", "b").build())

        matches(pattern, result!!)
    }

    @Test
    fun testDecorateUriWithParameters() {
        tracker.track(ScreenView("test"))

        val sessionId = session.sessionId
        val decorate = { c: CrossDeviceParameterConfiguration -> tracker.decorateLink(testLink, c)!! }

        matches(
            "http://example.com?_sp=$userId.$epoch.$sessionId",
            decorate(CrossDeviceParameterConfiguration(sourceId = false))
        )

        matches(
            "http://example.com?_sp=$userId.$epoch.$sessionId..$appId",
            decorate(CrossDeviceParameterConfiguration())
        )

        matches(
            "http://example.com?_sp=$userId.$epoch.$sessionId..$appId.mob",
            decorate(CrossDeviceParameterConfiguration(sourcePlatform = true))
        )

        matches(
            "http://example.com?_sp=$userId.$epoch.$sessionId.$subjectUserIdEncoded.$appId.mob",
            decorate(CrossDeviceParameterConfiguration(sourcePlatform = true, subjectUserId = true))
        )

        matches(
            "http://example.com?_sp=$userId.$epoch.$sessionId...mob",
            decorate(CrossDeviceParameterConfiguration(sourceId = false, sourcePlatform = true))
        )

        matches(
            "http://example.com?_sp=$userId.$epoch..$subjectUserIdEncoded.$appId",
            decorate(CrossDeviceParameterConfiguration(sessionId = false, subjectUserId = true))
        )

        matches(
            "http://example.com?_sp=$userId.$epoch..$subjectUserIdEncoded.$appId",
            decorate(CrossDeviceParameterConfiguration(sessionId = false, subjectUserId = true))
        )


        matches(
            "http://example.com?_sp=$userId.$epoch",
            decorate(CrossDeviceParameterConfiguration(sourceId = false, sessionId = false))
        )
    }

    private fun getTracker(): TrackerController {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val networkConfiguration = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))

        val trackerConfiguration = TrackerConfiguration("decoratorTest").sessionContext(true)

        val subjectConfig = SubjectConfiguration().userId(subjectUserId)

        val sessionConfiguration = SessionConfiguration(
            TimeMeasure(6, TimeUnit.SECONDS),
            TimeMeasure(30, TimeUnit.SECONDS),
        )

        return Snowplow.createTracker(
            context,
            "namespace" + Math.random(),
            networkConfiguration,
            trackerConfiguration,
            sessionConfiguration,
            subjectConfig
        )
    }

    private fun getTrackerNoSession(): TrackerController {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val networkConfiguration = NetworkConfiguration(MockNetworkConnection(HttpMethod.POST, 200))

        val trackerConfiguration = TrackerConfiguration("decoratorTest").sessionContext(false)

        return Snowplow.createTracker(
            context,
            "namespace" + Math.random(),
            networkConfiguration,
            trackerConfiguration,
        )
    }
}

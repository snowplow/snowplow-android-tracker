package com.snowplowanalytics.core.tracker

import com.snowplowanalytics.snowplow.util.Size

interface SubjectConfigurationInterface {
    var userId: String?
    var networkUserId: String?
    var domainUserId: String?
    var useragent: String?
    var ipAddress: String?
    var timezone: String?
    var language: String?
    var screenResolution: Size?
    var screenViewPort: Size?
    var colorDepth: Int?
}

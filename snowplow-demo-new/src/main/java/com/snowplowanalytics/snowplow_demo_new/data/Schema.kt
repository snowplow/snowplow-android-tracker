package com.snowplowanalytics.snowplow_demo_new.data

import org.json.JSONObject

data class Schema(
    val description: String?,
)

data class SchemaUrlParts(
    val url: String,
    val name: String,
    val vendor: String,
    val version: String,
)

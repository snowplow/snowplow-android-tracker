package com.snowplowanalytics.snowplow_demo_new.data

import org.json.JSONObject

data class Schema(
//    val url: String,
//    val name: String,
//    val vendor: String,
//    val version: String,
    val description: String?,
//    val json: JSONObject
)

data class SchemaUrlParts(
    val name: String,
    val vendor: String,
    val version: String,
)

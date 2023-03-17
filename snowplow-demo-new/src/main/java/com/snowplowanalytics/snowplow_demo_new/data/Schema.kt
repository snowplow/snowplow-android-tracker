package com.snowplowanalytics.snowplow_demo_new.data

data class Schema(
    val description: String?,
)

data class SchemaUrlParts(
    val url: String,
    val name: String,
    val vendor: String,
    val version: String,
)

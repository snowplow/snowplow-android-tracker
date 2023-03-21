package com.snowplowanalytics.snowplow_demo_new.ui

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowplowanalytics.snowplow_demo_new.data.IgluAPIService
import com.snowplowanalytics.snowplow_demo_new.data.SchemaUrlParts
import kotlinx.coroutines.launch
import org.json.JSONObject


class SchemaDetailViewModel : ViewModel() {
    
    var description: MutableState<String?> = mutableStateOf("")
    var json: MutableState<JSONObject> = mutableStateOf(JSONObject())
    
    fun getSchemaDescription(schemaUrl: String) {
        viewModelScope.launch {
            val apiService = IgluAPIService.getInstance()
            try {
                val schema = apiService.getSchemaDescription(schemaUrl)
                description.value = schema.description.toString()
            } catch (e: Exception) {
                println("❌ " + e.message.toString())
            }
        }
    }

    fun getSchemaJson(schemaUrl: String) {
        viewModelScope.launch {
            val apiService = IgluAPIService.getInstance()
            try {
                val responseBody = apiService.getSchemaJson(schemaUrl)
                json.value = JSONObject(responseBody.string())
            } catch (e: Exception) {
                println("❌ " + e.message.toString())
            }
        }
    }

    fun processSchemaUrl(schemaUrl: String) : SchemaUrlParts {
        val schemaParts = schemaUrl.split("/")
        return SchemaUrlParts(
            url = schemaUrl,
            name = schemaParts[1],
            vendor = schemaParts[0],
            version = schemaParts[3]
        )
    }
}

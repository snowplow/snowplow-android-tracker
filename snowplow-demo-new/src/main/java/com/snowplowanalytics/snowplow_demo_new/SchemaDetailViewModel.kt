package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowplowanalytics.snowplow_demo_new.data.Schema
import com.snowplowanalytics.snowplow_demo_new.data.SchemaUrlParts
import kotlinx.coroutines.launch
import org.json.JSONObject


class SchemaDetailViewModel : ViewModel() {
    
    var description: MutableState<String?> = mutableStateOf("initial value description")
    
    fun getSchemaDetails(schemaUrl: String) {
        println("❗️ here in getSchemaDetails, url is $schemaUrl")
        
        viewModelScope.launch {
            val apiService = IgluAPIService.getInstance()
            try {
                val schema = apiService.getSchema(schemaUrl)
                description.value = schema.description.toString()
                println("❗ " + description.value)

            } catch (e: Exception) {
                println("❗❌ " + e.message.toString())
            }
        }

    }

    fun processSchemaUrl(schemaUrl: String) : SchemaUrlParts {
        val schemaParts = schemaUrl.split("/")
        return SchemaUrlParts(
            name = schemaParts[1],
            vendor = schemaParts[0],
            version = schemaParts[3]
        )
    }
}

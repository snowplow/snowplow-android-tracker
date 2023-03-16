package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowplowanalytics.snowplow_demo_new.data.Schema
import com.snowplowanalytics.snowplow_demo_new.data.SchemaUrlParts
import kotlinx.coroutines.launch

class SchemaListViewModel : ViewModel() {
    var errorMessage: String by mutableStateOf("")

    private val _schemasPartsList = mutableStateListOf<SchemaUrlParts>()
    val schemaPartsList: List<SchemaUrlParts>
        get() = _schemasPartsList

    fun getSchemaList() {
        viewModelScope.launch {
            val apiService = IgluAPIService.getInstance()
            try {
                val schemas = apiService.getSchemas()
                for (schema in schemas) {
                    val schemaParts = schema.drop(5).split("/")
                    _schemasPartsList.add(SchemaUrlParts(
                        url = schema,
                        name = schemaParts[1],
                        vendor = schemaParts[0],
                        version = schemaParts[3]
                    ))
                }
            } catch (e: Exception) {
                errorMessage = e.message.toString()
            }
        }
        
    }
}

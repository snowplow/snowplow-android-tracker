package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snowplowanalytics.snowplow_demo_new.data.Schema
import kotlinx.coroutines.launch

class SchemaListViewModel : ViewModel() {
    var errorMessage: String by mutableStateOf("")
    
    private val _schemasList = mutableStateListOf<String>()
    val schemaList: List<String>
        get() = _schemasList

    private val _schemasPartsList = mutableStateListOf<HashMap<String, String>>()
    val schemaPartsList: List<HashMap<String, String>>
        get() = _schemasPartsList
    

    fun getSchemaList() {
        viewModelScope.launch {
            val apiService = IgluAPIService.getInstance()
            try {
                val schemas = apiService.getSchemas()
                _schemasList.clear()
                _schemasList.addAll(schemas)
                
                for (schema in schemas) {
                    val schemaParts = schema.drop(5).split("/")
                    val schemaHash = HashMap<String, String>()
                    schemaHash["url"] = schema
                    schemaHash["name"] = schemaParts[1]
                    schemaHash["vendor"] = schemaParts[0]
                    schemaHash["version"] = schemaParts[3]
                    _schemasPartsList.add(schemaHash)
                }
                
            } catch (e: Exception) {
                errorMessage = e.message.toString()
            }
        }
        
    }
}

package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class Todo(
    var userId: Int,
    var id: Int,
    var title: String,
    var completed: Boolean
)

const val BASE_URL = "https://jsonplaceholder.typicode.com/"

interface TodoAPIService {
    @GET("todos")
    suspend fun getTodos(): List<Todo>
    
    companion object {
        var todoApiService: TodoAPIService? = null
        
        fun getInstance(): TodoAPIService {
            if (todoApiService == null) {
                todoApiService = Retrofit.Builder()
                    .baseUrl(IGLU_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(TodoAPIService::class.java)
            }
            return todoApiService!!
        }
    }
}

class TodoViewModel : ViewModel() {
    private val _todoList = mutableStateListOf<Todo>()
    var errorMessage: String by mutableStateOf("")
    val todoList: List<Todo>
        get() = _todoList
    
    fun getTodoList() {
        viewModelScope.launch {
            val apiService = TodoAPIService.getInstance()
            try {
                _todoList.clear()
                _todoList.addAll(apiService.getTodos())
            } catch (e: Exception) {
                errorMessage = e.message.toString()
            }
        }
    }
}

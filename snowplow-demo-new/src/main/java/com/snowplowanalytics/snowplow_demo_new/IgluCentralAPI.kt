package com.snowplowanalytics.snowplow_demo_new

import androidx.compose.runtime.Composable
import okhttp3.*
import java.io.IOException
import java.lang.Exception

object IgluCentralAPI {
    private val client = OkHttpClient()
    private val endpoint = "http://iglucentral.com"

    @Composable
    fun schemas() {
        println("♦️♦️ here in schemas")
        val request = Request.Builder()
            .url("$endpoint/schemas")
            .build()
        
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("♦️ Unexpected code $response")
                    
//                    for ((name, value) in response.headers) {
//                        println("♦️ $name: $value")
//                    }
                    val responseBody = response.body
                    println("♦️♦️ " + responseBody!!.string().split(",").size)
//                    val responseItems = responseBody.string().split(",")
//                    println("❗️ " + responseItems.size)
                }
            }
        })
        
    }

    @Composable
    fun schema(schemaUrl: String) : String {
        println("♦️♦️ here in single schema")
        
        val schemaPath = schemaUrl.drop(5)
        val url = "$endpoint/schemas/$schemaPath"

        val request = Request.Builder()
            .url(url)
            .build()
        
        val responseString = "starting string"

//        curl http://iglucentral.com/schemas/com.amazon.aws.lambda/java_context/jsonschema/1-0-0 -X GET
        
        
//        client.newCall(request).enqueue(object: Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                e.printStackTrace()
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                response.use {
//                    if (!response.isSuccessful) throw IOException("♦️ Unexpected code $response")
//
////                    for ((name, value) in response.headers) {
////                        println("♦️ $name: $value")
////                    }
//                    responseString = response.body.toString()
//                    println("♦️️ $responseString")
//                }
//            }
//        })
//        println("♦️♦️ $responseString")
        return responseString
    }


    suspend fun singleSchemaBody(schemaUrl: String) : String {
        println("♦️♦️ here in single schema body")

        val schemaPath = schemaUrl.drop(5)
        val url = "$endpoint/schemas/$schemaPath"

        val request = Request.Builder()
            .url(url)
            .build()

        var responseString = "ugh"
        
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("♦️ Unexpected code $response")

//                    for ((name, value) in response.headers) {
//                        println("♦️ $name: $value")
//                    }
                    responseString = response.body.toString()
                    
                }
            }
        })
        return responseString
    }

}

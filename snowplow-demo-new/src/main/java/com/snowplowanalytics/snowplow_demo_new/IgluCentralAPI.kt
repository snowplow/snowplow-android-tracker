package com.snowplowanalytics.snowplow_demo_new

import com.snowplowanalytics.snowplow_demo_new.data.Schema
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path


const val IGLU_BASE_URL = "http://iglucentral.com/"

interface IgluAPIService {
    @GET("schemas")
    suspend fun getSchemas(): List<String>

    @GET("schemas/{schemaUrl}")
    suspend fun getSchema(@Path("schemaUrl") schemaUrl: String): Schema

    companion object {
        var igluApiService: IgluAPIService? = null

        fun getInstance(): IgluAPIService {
            if (igluApiService == null) {
                igluApiService = Retrofit.Builder()
                    .baseUrl(IGLU_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(IgluAPIService::class.java)
            }
            return igluApiService!!
        }
    }
}

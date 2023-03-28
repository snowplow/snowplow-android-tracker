package com.snowplowanalytics.snowplowdemocompose.data

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

const val IGLU_BASE_URL = "http://iglucentral.com/"

interface IgluAPIService {
    @GET("schemas")
    suspend fun getSchemas(): List<String>

    @GET("schemas/{schemaUrl}")
    suspend fun getSchemaDescription(@Path("schemaUrl") schemaUrl: String): SchemaDescription

    @GET("schemas/{schemaUrl}")
    suspend fun getSchemaJson(@Path("schemaUrl") schemaUrl: String): ResponseBody

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

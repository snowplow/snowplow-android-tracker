package com.snowplowanalytics.snowplow_demo_new

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET


const val IGLU_BASE_URL = "http://iglucentral.com/"

interface IgluAPIService {
    @GET("schemas")
    suspend fun getSchemas(): List<String>

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

package com.example.dmarketalert.model.remote

import com.example.dmarketalert.model.TargetsApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MarketApiService {
    @GET("api/v1/targets/current")
    suspend fun getActiveTargets(
        @Header("Authorization") apiKey: String,
        @Query("status") status: String = "active"
    ): Response<TargetsApiResponse>

    @GET("api/v1/targets/current")
    suspend fun getTargetsHistory(
        @Header("Authorization") apiKey: String,
        @Query("limit") limit: Int = 100
    ): Response<TargetsApiResponse>

    @GET("api/v1/targets/current")
    suspend fun refreshTargets(
        @Header("Authorization") apiKey: String
    ): Response<TargetsApiResponse>

    @GET("api/v1/validate")
    suspend fun validateApiKey(
        @Header("Authorization") apiKey: String
    ): Response<Unit>
}
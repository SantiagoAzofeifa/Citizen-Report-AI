package com.example.citizenreportai.data.remote

import com.example.citizenreportai.data.model.Report
import com.example.citizenreportai.data.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("users")
    suspend fun getUsers(): List<User>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: String): User

    @GET("reports")
    suspend fun getReports(): List<Report>

    @POST("reports")
    suspend fun createReport(@Body report: Report): Report
}

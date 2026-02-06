package com.agentasker.core.network

import com.agentasker.features.login.data.datasources.remote.model.AuthResponseDTO
import com.agentasker.features.login.data.datasources.remote.model.GoogleSignInRequestDTO
import com.agentasker.features.login.data.datasources.remote.model.LoginRequestDTO
import com.agentasker.features.login.data.datasources.remote.model.LoginResponseDTO
import com.agentasker.features.login.data.datasources.remote.model.ProfileResponseDTO
import com.agentasker.features.login.data.datasources.remote.model.RegisterRequestDTO
import com.agentasker.features.login.data.datasources.remote.model.RegisterResponseDTO
import com.agentasker.features.tasks.data.datasources.remote.model.CreateTaskRequest
import com.agentasker.features.tasks.data.datasources.remote.model.TaskDTO
import com.agentasker.features.tasks.data.datasources.remote.model.TaskResponse
import com.agentasker.features.tasks.data.datasources.remote.model.UpdateTaskRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AgentTaskerApi {

    @GET("tasks")
    suspend fun getTasks(): TaskResponse

    @GET("tasks/{id}")
    suspend fun getTaskById(@Path("id") id: Int): TaskDTO

    @POST("tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): TaskDTO

    @PATCH("tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body request: UpdateTaskRequest): TaskDTO

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    @POST("auth/google")
    suspend fun signInWithGoogle(@Body request: GoogleSignInRequestDTO): AuthResponseDTO

    @GET("auth/me")
    suspend fun getCurrentUser(): AuthResponseDTO

    @POST("users/login")
    suspend fun login(@Body request: LoginRequestDTO): LoginResponseDTO

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequestDTO): RegisterResponseDTO

    @GET("users/profile")
    suspend fun getProfile(): ProfileResponseDTO

}



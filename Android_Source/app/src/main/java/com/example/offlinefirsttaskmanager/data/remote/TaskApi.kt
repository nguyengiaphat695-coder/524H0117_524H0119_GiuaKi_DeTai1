
package com.example.offlinefirsttaskmanager.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class HealthResponse(
    val status: String
)

data class TaskResponse(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
    val updatedAt: Long,
    val serverVersion: Long
)

data class CreateTaskRequest(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)

data class UpdateTaskRequest(
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val expectedVersion: Long
)

interface TaskApi {

    @GET("health")
    suspend fun checkHealth(): HealthResponse

    @GET("tasks")
    suspend fun getTasks(): List<TaskResponse>

    @GET("tasks/{task_id}")
    suspend fun getTask(
        @Path("task_id") id: String
    ): TaskResponse

    @POST("tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest,
        @Header("Idempotency-Key") key: String
    ): TaskResponse

    @PUT("tasks/{task_id}")
    suspend fun updateTask(
        @Path("task_id") id: String,
        @Body request: UpdateTaskRequest,
        @Header("Idempotency-Key") key: String
    ): TaskResponse

    @DELETE("tasks/{task_id}")
    suspend fun deleteTask(
        @Path("task_id") id: String,
        @Query("expectedVersion") version: Long,
        @Header("Idempotency-Key") key: String
    ): TaskResponse
}

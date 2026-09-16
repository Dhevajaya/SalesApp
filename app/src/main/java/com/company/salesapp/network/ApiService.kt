package com.company.salesapp.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Daftar endpoint sesuai Section 15 blueprint.
 * Identity Sales ditentukan Laravel dari token/session — TIDAK ada sales_id
 * dikirim manual sebagai parameter path/query dari Android (Section 28 - Security).
 */
interface ApiService {

    @GET("api/sales/tasks/current")
    suspend fun getCurrentTask(): Response<CurrentTaskResponse>

    @GET("api/sales/tasks/{id}/documents")
    suspend fun getTaskDocuments(@Path("id") taskId: Long): Response<Any>

    @GET("api/sales/tasks/{id}/stock")
    suspend fun getTaskStock(@Path("id") taskId: Long): Response<Any>

    @POST("api/sales/tasks/{id}/verify-stock")
    suspend fun verifyStock(@Path("id") taskId: Long): Response<GenericSuccessResponse>

    @POST("api/sales/tasks/{id}/start-work")
    suspend fun startWork(@Path("id") taskId: Long): Response<StartWorkResponse>

    @POST("api/sales/tracking/start")
    suspend fun startTracking(): Response<TrackingStartResponse>

    @POST("api/sales/tracking/stop")
    suspend fun stopTracking(): Response<TrackingStopResponse>

    @POST("api/sales/location")
    suspend fun postLocation(@Body body: LocationEventRequest): Response<LocationEventResponse>

    @GET("api/sales/tracking/status")
    suspend fun getTrackingStatus(): Response<TrackingStatusResponse>
}

package com.example.mychatapp.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {
    @FormUrlEncoded
    @POST("auth")
    suspend fun authenticate(
        @Field("username") username: String,
        @Field("password") password: String
    ): AuthResponse

    @Streaming
    @POST("chat")
    suspend fun chatStream(
        @Query("prompt") prompt: String
    ): Response<ResponseBody>
}

data class AuthResponse(
    val access_token: String,
    val token_type: String
)
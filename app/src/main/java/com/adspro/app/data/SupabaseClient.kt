package com.adspro.app.data

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

object SupabaseClient {
    private const val BASE_URL = "https://bqddohfszkqpvncpbrun.supabase.co"
    private const val REST_URL = "$BASE_URL/rest/v1"
    private const val STORAGE_URL = "$BASE_URL/storage/v1"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJxZGRvaGZzemtxcHZuY3BicnVuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTI2MDAwNjIsImV4cCI6MjEwODE3NjA2Mn0.J0uq4JPpPxoU0hc4ry5WglCV_HcTqshcHsl3oCJ7upA"

    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun insert(table: String, payload: Any): Boolean {
        val jsonString = if (payload is String) payload else gson.toJson(payload)
        val request = Request.Builder().url("$REST_URL/$table").post(jsonString.toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("apikey", ANON_KEY).addHeader("Authorization", "Bearer $ANON_KEY")
            .addHeader("Content-Type", "application/json").addHeader("Prefer", "return=minimal").build()
        return try { client.newCall(request).execute().use { it.isSuccessful } } catch (e: Exception) { false }
    }

    fun uploadFile(bucket: String, path: String, file: File, mimeType: String): String? {
        val request = Request.Builder().url("$STORAGE_URL/object/$bucket/$path").post(file.asRequestBody(mimeType.toMediaType()))
            .addHeader("apikey", ANON_KEY).addHeader("Authorization", "Bearer $ANON_KEY").addHeader("x-upsert", "true").build()
        return try {
            client.newCall(request).execute().use { if (it.isSuccessful) "$STORAGE_URL/object/public/$bucket/$path" else null }
        } catch (e: Exception) { null }
    }

    fun listMedia(bucket: String, pathPrefix: String = ""): List<String> {
        val payload = mapOf("prefix" to pathPrefix, "limit" to 100, "offset" to 0)
        val request = Request.Builder().url("$STORAGE_URL/object/list/$bucket").post(gson.toJson(payload).toRequestBody(JSON_MEDIA_TYPE))
            .addHeader("apikey", ANON_KEY).addHeader("Authorization", "Bearer $ANON_KEY").build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val items = gson.fromJson(response.body?.string(), Array<StorageItem>::class.java)
                    items.map { "$STORAGE_URL/object/public/$bucket/${it.name}" }
                } else emptyList()
            }
        } catch (e: Exception) { emptyList() }
    }
    private data class StorageItem(val name: String)
}

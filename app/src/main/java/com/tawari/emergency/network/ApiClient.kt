package com.tawari.emergency.network

import android.content.Context
import android.net.wifi.WifiManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tawari.emergency.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context) {

    companion object {
        // جميع عناوين Emergency Nodes للقريتين A و B
        private val EMERGENCY_NODE_IPS = listOf(
            // قرية A
            "192.168.10.1",   // Emergency 1-A
            "192.168.11.1",   // Emergency 2-A
            // قرية B
            "192.168.12.1",   // Emergency 1-B
            "192.168.13.1"    // Emergency 2-B
        )
        
        // أسماء شبكات القرى المدعومة
        private val VILLAGE_SSIDS = listOf(
            // قرية A
            "Tawari-Qarya-A",
            "Tawari-Qarya-A2",
            "Center-A-Network",
            // قرية B
            "Tawari-Qarya-B",
            "Tawari-Qarya-B2",
            "Center-B-Network"
        )
        
        private const val DEVICE_NAME = "TawariApp-Android"
        private const val TIMEOUT_SECONDS = 10L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private var connectedNodeIp: String? = null
    private var connectedVillage: String = ""

    // التحقق من الاتصال بشبكة القرية
    fun isConnectedToVillageNetwork(): Boolean {
        val ssid = getCurrentSSID() ?: return false
        return VILLAGE_SSIDS.any { ssid.contains(it, ignoreCase = true) }
    }

    // الحصول على اسم الشبكة الحالية
    fun getCurrentSSID(): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.connectionInfo?.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" }
        } catch (e: Exception) {
            null
        }
    }

    // تحديد القرية من اسم الشبكة
    fun detectVillage(): String {
        val ssid = getCurrentSSID() ?: return ""
        return when {
            ssid.contains("Qarya-A", true) || ssid.contains("Center-A", true) -> "A"
            ssid.contains("Qarya-B", true) || ssid.contains("Center-B", true) -> "B"
            else -> ""
        }
    }

    // البحث عن Emergency Node والاتصال به
    suspend fun findEmergencyNode(): String? = withContext(Dispatchers.IO) {
        // تحديد IPs حسب القرية المتصل بها
        val village = detectVillage()
        val ipsToTry = when (village) {
            "A" -> listOf("192.168.10.1", "192.168.11.1")
            "B" -> listOf("192.168.12.1", "192.168.13.1")
            else -> EMERGENCY_NODE_IPS
        }

        for (ip in ipsToTry) {
            try {
                val request = Request.Builder()
                    .url("http://$ip/api/status")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        connectedNodeIp = ip
                        connectedVillage = village
                        return@withContext ip
                    }
                }
            } catch (e: Exception) {
                // جرب العنوان التالي
            }
        }
        connectedNodeIp = null
        return@withContext null
    }

    fun getConnectedVillage(): String = connectedVillage
    fun getConnectedNodeIp(): String? = connectedNodeIp

    private fun getBaseUrl(): String? {
        return connectedNodeIp?.let { "http://$it" }
    }

    // إرسال بلاغ طوارئ
    suspend fun sendEmergencyReport(
        type: String,
        location: String,
        note: String = ""
    ): Result<ReportResponse> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: return@withContext Result.failure(Exception("غير متصل بالشبكة"))

        try {
            val parts = location.split(",")
            val lat = parts.getOrNull(0)?.trim() ?: ""
            val lon = parts.getOrNull(1)?.trim() ?: ""

            val encodedType = URLEncoder.encode(type, "UTF-8")
            val encodedNote = URLEncoder.encode(note, "UTF-8")
            val url = "$baseUrl/report?type=$encodedType&lat=$lat&lon=$lon&note=$encodedNote"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(ReportResponse(
                        id = System.currentTimeMillis().toInt(),
                        status = "stored",
                        nodeReportId = 0
                    ))
                } else {
                    Result.failure(Exception("فشل الإرسال: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // جلب قائمة المحتوى
    suspend fun getContent(): Result<List<ContentItem>> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: return@withContext Result.failure(Exception("غير متصل"))

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/content")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<ContentItem>>() {}.type
                    val list: List<ContentItem> = gson.fromJson(body, type)
                    Result.success(list)
                } else {
                    Result.failure(Exception("فشل التحميل"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // جلب قائمة العملاء
    suspend fun getAgents(): Result<List<Agent>> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: return@withContext Result.failure(Exception("غير متصل"))

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/agents")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<Agent>>() {}.type
                    val list: List<Agent> = gson.fromJson(body, type)
                    Result.success(list)
                } else {
                    Result.failure(Exception("فشل التحميل"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // إرسال رسالة محادثة
    suspend fun sendChatMessage(
        session: String,
        from: String,
        text: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: return@withContext Result.failure(Exception("غير متصل"))

        try {
            val json = gson.toJson(mapOf(
                "session" to session,
                "from" to from,
                "text" to text,
                "node" to DEVICE_NAME
            ))

            val request = Request.Builder()
                .url("$baseUrl/api/chat/send")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // تحميل رسائل المحادثة
    suspend fun loadChatMessages(session: String): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: return@withContext Result.failure(Exception("غير متصل"))

        try {
            val encodedSession = URLEncoder.encode(session, "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/api/chat/load?session=$encodedSession")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val type = object : TypeToken<List<ChatMessage>>() {}.type
                    val list: List<ChatMessage> = gson.fromJson(body, type)
                    Result.success(list)
                } else {
                    Result.failure(Exception("فشل التحميل"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // الحصول على حالة العقدة
    suspend fun getNodeStatus(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl() ?: return@withContext Result.failure(Exception("غير متصل"))

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/status")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    val map: Map<String, Any> = gson.fromJson(body, type)
                    Result.success(map)
                } else {
                    Result.failure(Exception("فشل"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

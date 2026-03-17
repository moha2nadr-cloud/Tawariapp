package com.tawari.emergency.models

import com.google.gson.annotations.SerializedName

// Emergency Report
data class EmergencyReport(
    val id: Int = 0,
    val type: String,
    val location: String,
    val note: String = "",
    val status: Int = 0, // 0=انتظار, 1=نشط, 2=مكتمل
    val synced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// Content from Center
data class ContentItem(
    val kind: String,
    val title: String,
    val body: String,
    val extra: String = ""
)

// Agent
data class Agent(
    val name: String,
    val status: String // متاح / مشغول
) {
    val isAvailable: Boolean get() = status == "متاح"
}

// Chat Message
data class ChatMessage(
    val from: String,
    val text: String,
    val ts: Long = System.currentTimeMillis()
)

// API Responses
data class ReportResponse(
    val id: Int,
    val status: String,
    val nodeReportId: Int
)

data class RegisterResponse(
    val status: String,
    val center: String,
    val networkEnabled: Boolean
)

data class HeartbeatResponse(
    val ok: Boolean,
    val networkEnabled: Boolean
)

// Network State
enum class NetworkState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    NETWORK_DISABLED
}

// Emergency Types
enum class EmergencyType(val arabicName: String, val color: Int) {
    SOS("SOS", 0xFFDC2626.toInt()),
    FIRE("حريق", 0xFF7F1D1D.toInt()),
    INJURY("اصابة", 0xFF166534.toInt()),
    THREAT("تهديد", 0xFF312E81.toInt()),
    ACCIDENT("حادث", 0xFF1E3A6E.toInt()),
    TRAPPED("احتجاز", 0xFF713F12.toInt()),
    OTHER("اخرى", 0xFF1F2937.toInt())
}

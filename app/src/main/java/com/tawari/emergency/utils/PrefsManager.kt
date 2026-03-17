package com.tawari.emergency.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tawari.emergency.models.EmergencyReport

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "tawari_prefs",
        Context.MODE_PRIVATE
    )

    private val gson = Gson()

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_SAVED_LOCATION = "saved_location"
        private const val KEY_REPORTS = "reports"
        private const val KEY_LAST_CONNECTED_NODE = "last_node"
    }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var savedLocation: String
        get() = prefs.getString(KEY_SAVED_LOCATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_LOCATION, value).apply()

    var lastConnectedNode: String
        get() = prefs.getString(KEY_LAST_CONNECTED_NODE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTED_NODE, value).apply()

    fun saveReport(report: EmergencyReport) {
        val reports = getReports().toMutableList()
        reports.add(0, report)
        if (reports.size > 50) reports.removeLast() // Keep last 50 reports
        prefs.edit().putString(KEY_REPORTS, gson.toJson(reports)).apply()
    }

    fun getReports(): List<EmergencyReport> {
        val json = prefs.getString(KEY_REPORTS, "[]") ?: "[]"
        val type = object : TypeToken<List<EmergencyReport>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun updateReportStatus(reportId: Int, status: Int) {
        val reports = getReports().toMutableList()
        val index = reports.indexOfFirst { it.id == reportId }
        if (index >= 0) {
            reports[index] = reports[index].copy(status = status)
            prefs.edit().putString(KEY_REPORTS, gson.toJson(reports)).apply()
        }
    }

    fun clearReports() {
        prefs.edit().putString(KEY_REPORTS, "[]").apply()
    }
}

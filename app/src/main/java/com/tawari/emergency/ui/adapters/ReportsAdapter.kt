package com.tawari.emergency.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tawari.emergency.R
import com.tawari.emergency.models.EmergencyReport
import java.text.SimpleDateFormat
import java.util.*

class ReportsAdapter : ListAdapter<EmergencyReport, ReportsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<EmergencyReport>() {
        override fun areItemsTheSame(old: EmergencyReport, new: EmergencyReport) = old.id == new.id
        override fun areContentsTheSame(old: EmergencyReport, new: EmergencyReport) = old == new
    }
) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val type: TextView = view.findViewById(R.id.reportType)
        val status: TextView = view.findViewById(R.id.reportStatus)
        val time: TextView = view.findViewById(R.id.reportTime)
        val location: TextView = view.findViewById(R.id.reportLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = getItem(position)
        
        holder.type.text = report.type
        holder.status.text = when (report.status) {
            0 -> "في الانتظار"
            1 -> "تحت الاستجابة"
            2 -> "مكتمل"
            else -> "غير معروف"
        }
        holder.status.setBackgroundResource(
            when (report.status) {
                0 -> R.drawable.bg_status_pending
                1 -> R.drawable.bg_status_active
                2 -> R.drawable.bg_status_done
                else -> R.drawable.bg_status_pending
            }
        )
        
        val dateFormat = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
        holder.time.text = dateFormat.format(Date(report.timestamp))
        holder.location.text = if (report.location.isNotEmpty()) report.location else "غير محدد"
    }
}

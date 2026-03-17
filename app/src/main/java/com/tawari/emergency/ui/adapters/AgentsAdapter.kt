package com.tawari.emergency.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tawari.emergency.R
import com.tawari.emergency.models.Agent

class AgentsAdapter(
    private val onChatClick: (Agent) -> Unit
) : ListAdapter<Agent, AgentsAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Agent>() {
        override fun areItemsTheSame(old: Agent, new: Agent) = old.name == new.name
        override fun areContentsTheSame(old: Agent, new: Agent) = old == new
    }
) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.agentName)
        val status: TextView = view.findViewById(R.id.agentStatus)
        val chatButton: Button = view.findViewById(R.id.chatButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val agent = getItem(position)
        
        holder.name.text = agent.name
        holder.status.text = agent.status
        holder.status.setTextColor(
            holder.itemView.context.getColor(
                if (agent.isAvailable) R.color.green else R.color.yellow
            )
        )
        
        holder.chatButton.visibility = if (agent.isAvailable) View.VISIBLE else View.GONE
        holder.chatButton.setOnClickListener { onChatClick(agent) }
    }
}

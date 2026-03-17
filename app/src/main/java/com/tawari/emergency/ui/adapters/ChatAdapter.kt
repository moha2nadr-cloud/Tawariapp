package com.tawari.emergency.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tawari.emergency.R
import com.tawari.emergency.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val currentUser: String
) : ListAdapter<ChatMessage, ChatAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = 
            old.ts == new.ts && old.from == new.from
        override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
    }
) {

    private val messages = mutableListOf<ChatMessage>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val messageTime: TextView = view.findViewById(R.id.messageTime)
        val senderName: TextView? = view.findViewById(R.id.senderName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        
        holder.messageText.text = message.text
        
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.messageTime.text = dateFormat.format(Date(message.ts))
        
        if (message.from != currentUser) {
            holder.senderName?.text = message.from
            holder.senderName?.visibility = View.VISIBLE
        } else {
            holder.senderName?.visibility = View.GONE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).from == currentUser) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        submitList(messages.toList())
    }

    override fun submitList(list: List<ChatMessage>?) {
        messages.clear()
        messages.addAll(list ?: emptyList())
        super.submitList(messages.toList())
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}

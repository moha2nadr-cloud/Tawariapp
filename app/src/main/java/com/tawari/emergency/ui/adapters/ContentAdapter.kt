package com.tawari.emergency.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tawari.emergency.R
import com.tawari.emergency.models.ContentItem

class ContentAdapter : ListAdapter<ContentItem, ContentAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ContentItem>() {
        override fun areItemsTheSame(old: ContentItem, new: ContentItem) = 
            old.title == new.title && old.kind == new.kind
        override fun areContentsTheSame(old: ContentItem, new: ContentItem) = old == new
    }
) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.contentTitle)
        val body: TextView = view.findViewById(R.id.contentBody)
        val kind: TextView = view.findViewById(R.id.contentKind)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_content, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        holder.title.text = item.title
        holder.body.text = item.body
        holder.kind.text = when (item.kind) {
            "instruction" -> "تعليمات"
            "safety" -> "إرشادات السلامة"
            "shelter" -> "ملجأ"
            "faq" -> "أسئلة شائعة"
            else -> item.kind
        }
        
        // Set background based on kind
        holder.itemView.setBackgroundResource(
            when (item.kind) {
                "instruction" -> R.drawable.bg_content_instruction
                "safety" -> R.drawable.bg_content_safety
                "shelter" -> R.drawable.bg_content_shelter
                else -> R.drawable.bg_content_default
            }
        )
    }
}

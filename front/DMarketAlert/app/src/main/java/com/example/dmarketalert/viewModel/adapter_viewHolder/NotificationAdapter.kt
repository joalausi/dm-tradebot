package com.example.dmarketalert.viewModel.adapter_viewHolder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.dmarketalert.R
import com.example.dmarketalert.model.local.NotificationEntity

class NotificationAdapter : ListAdapter<NotificationEntity, NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
        holder.title.text = item.title
        holder.message.text = item.message
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
    override fun areItemsTheSame(
        oldItem: NotificationEntity,
        newItem: NotificationEntity
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: NotificationEntity,
        newItem: NotificationEntity
    ): Boolean = oldItem == newItem
}
package com.example.dmarketalert.viewModel.adapter_viewHolder

import android.view.View
import android.widget.TextView
import com.example.dmarketalert.R
import androidx.recyclerview.widget.RecyclerView

class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title: TextView = view.findViewById(R.id.textView_noti_title)
    val message: TextView = view.findViewById(R.id.textView_noti_message)
}
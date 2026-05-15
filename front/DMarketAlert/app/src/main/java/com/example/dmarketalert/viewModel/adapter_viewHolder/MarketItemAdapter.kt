package com.example.dmarketalert.viewModel.adapter_viewHolder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.example.dmarketalert.R
import com.example.dmarketalert.model.MarketItem

class MarketItemAdapter(
    private val onItemClick: (MarketItem) -> Unit = {}
) : ListAdapter<MarketItem, MarketItemViewHolder>(MarketItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_target, parent, false)
        return MarketItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarketItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }
}
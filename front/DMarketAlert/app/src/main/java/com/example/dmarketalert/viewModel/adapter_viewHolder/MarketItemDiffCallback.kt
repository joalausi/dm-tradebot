package com.example.dmarketalert.viewModel.adapter_viewHolder

import androidx.recyclerview.widget.DiffUtil
import com.example.dmarketalert.model.MarketItem

class MarketItemDiffCallback : DiffUtil.ItemCallback<MarketItem>() {
    override fun areItemsTheSame(oldItem: MarketItem, newItem: MarketItem): Boolean {
        return oldItem.gameId == newItem.gameId
    }

    override fun areContentsTheSame(oldItem: MarketItem, newItem: MarketItem): Boolean {
        return oldItem == newItem
    }
}
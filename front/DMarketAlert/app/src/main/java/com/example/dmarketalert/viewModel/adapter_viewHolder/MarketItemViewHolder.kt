package com.example.dmarketalert.viewModel.adapter_viewHolder

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.dmarketalert.R
import com.example.dmarketalert.model.MarketItem

class MarketItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val tvItemTitle: TextView = itemView.findViewById(R.id.tv_item_title)
    private val tvItemCondition: TextView = itemView.findViewById(R.id.tv_item_condition)
    private val tvPriceChange: TextView = itemView.findViewById(R.id.tv_price_change)
    private val tvTargetPrice: TextView = itemView.findViewById(R.id.tv_target_price)
    private val tvMaxPrice: TextView = itemView.findViewById(R.id.tv_max_price)
    private val tvOfferPrice: TextView = itemView.findViewById(R.id.tv_offer_price)
    private val tvQuantity: TextView = itemView.findViewById(R.id.tv_quantity)
    private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)

    fun bind(item: MarketItem) {
        tvItemTitle.text = item.title

        if (item.condition.isNotEmpty()) {
            tvItemCondition.text = item.condition
            tvItemCondition.visibility = View.VISIBLE
        } else {
            tvItemCondition.visibility = View.GONE
        }

        tvPriceChange.text = item.getPriceChangeText()
        val priceChangeColor = if (item.priceChangePercent >= 0) {
            ContextCompat.getColor(itemView.context, android.R.color.holo_green_light)
        } else {
            ContextCompat.getColor(itemView.context, android.R.color.holo_red_light)
        }
        tvPriceChange.setTextColor(priceChangeColor)

        tvTargetPrice.text = item.formatPrice(item.myTargetUsd)
        tvMaxPrice.text = item.formatPrice(item.maxTargetUsd)
        tvOfferPrice.text = item.formatPrice(item.bestOfferUsd)

        tvQuantity.text = item.myQty.toString()

        tvStatus.text = item.getDisplayStatus()
        val statusColor = if (item.isOutbid) {
            ContextCompat.getColor(itemView.context, android.R.color.holo_red_light)
        } else {
            ContextCompat.getColor(itemView.context, android.R.color.holo_green_light)
        }
        tvStatus.setTextColor(statusColor)
    }
}
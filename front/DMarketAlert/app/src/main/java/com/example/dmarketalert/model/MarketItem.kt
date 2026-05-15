package com.example.dmarketalert.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MarketItem(
    val gameId: String,
    val title: String,
    val condition: String,
    val targetStatus: String,
    val myTargetUsd: Double,
    val maxTargetUsd: Double,
    val bestOfferUsd: Double,
    val myQty: Int,
    val targetQtyTotal: Int,
    val isActive: Boolean,
    val isOutbid: Boolean,
    val priceChangePercent: Double,
    val priceChangeAmount: Double,
    val timestamp: Long
) : Parcelable {
    fun getDisplayStatus(): String = if (isOutbid) "Outbid" else "Active"

    fun getStatusColorRes(): Int = if (isOutbid) {
        android.R.color.holo_red_light
    } else {
        android.R.color.holo_green_light
    }
    fun getPriceChangeText(): String {
        val sign = if (priceChangePercent >= 0) "+" else ""
        return "${sign}${String.format("%.1f", priceChangePercent)}%"
    }
    fun getPriceChangeColorRes(): Int = if (priceChangePercent >= 0) {
        android.R.color.holo_green_light
    } else {
        android.R.color.holo_red_light
    }
    fun formatPrice(price: Double): String = "$${String.format("%.2f", price)}"
}
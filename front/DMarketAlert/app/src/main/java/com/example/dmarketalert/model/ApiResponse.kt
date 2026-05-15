package com.example.dmarketalert.model

import com.google.gson.annotations.SerializedName

data class TargetsApiResponse(
    @SerializedName("items")
    val items: List<MarketItemDto>
)

data class MarketItemDto(
    @SerializedName("title")
    val title: String,

    @SerializedName("game_id")
    val gameId: String,

    @SerializedName("target_status")
    val targetStatus: String,

    @SerializedName("my_target_usd")
    val myTargetUsd: Double,

    @SerializedName("max_target_usd")
    val maxTargetUsd: Double,

    @SerializedName("best_offer_usd")
    val bestOfferUsd: Double,

    @SerializedName("my_qty")
    val myQty: Int,

    @SerializedName("target_qty_total")
    val targetQtyTotal: Int,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("is_outbid")
    val isOutbid: Boolean
)

fun MarketItemDto.toDomainModel(): MarketItem {
    val (cleanTitle, condition) = parseTitle(title)

    val priceChangePercent = calculatePriceChange(myTargetUsd, bestOfferUsd)
    val priceChangeAmount = bestOfferUsd - myTargetUsd

    return MarketItem(
        gameId = gameId,
        title = cleanTitle,
        condition = condition,
        targetStatus = targetStatus,
        myTargetUsd = myTargetUsd,
        maxTargetUsd = maxTargetUsd,
        bestOfferUsd = bestOfferUsd,
        myQty = myQty,
        targetQtyTotal = targetQtyTotal,
        isActive = isActive,
        isOutbid = isOutbid,
        priceChangePercent = priceChangePercent,
        priceChangeAmount = priceChangeAmount,
        timestamp = System.currentTimeMillis()
    )
}
private fun parseTitle(title: String): Pair<String, String> {
    val regex = """(.*?)\s*\((.*?)\)""".toRegex()
    val matchResult = regex.find(title)

    return if (matchResult != null) {
        val cleanTitle = matchResult.groupValues[1].trim()
        val condition = matchResult.groupValues[2].trim()
        Pair(cleanTitle, condition)
    } else {
        Pair(title, "")
    }
}
private fun calculatePriceChange(myTarget: Double, bestOffer: Double): Double {
    return if (myTarget > 0) {
        ((bestOffer - myTarget) / myTarget) * 100
    } else {
        0.0
    }
}

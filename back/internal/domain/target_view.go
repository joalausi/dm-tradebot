package domain

type TargetView struct {
	Title          string  `json:"title"`
	GameID         string  `json:"game_id"`
	TargetStatus  string  `json:"target_status"`
	MyTargetUSD   float64 `json:"my_target_usd"`
	MaxTargetUSD  float64 `json:"max_target_usd"`
	BestOfferUSD  float64 `json:"best_offer_usd"`
	MyQty          int     `json:"my_qty"`
	TargetQtyTotal int    `json:"target_qty_total"`
	ROIUSD         float64 `json:"roi_usd"`
	ROIPercent     float64 `json:"roi_percent"`
	IsActive        bool    `json:"is_active"`
	IsOutbid        bool    `json:"is_outbid"`
}
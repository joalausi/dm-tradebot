package domain

import "time"

type Opportunity struct {
	ScannedAt time.Time `json:"scanned_at"`

	GameID string `json:"game_id"`
	Title  string `json:"title"`

	BestTargetUSD float64 `json:"best_target_usd"`
	TargetCount   int     `json:"target_count"`

	BestOfferUSD float64 `json:"best_offer_usd"`
	OfferCount   int     `json:"offer_count"`

	LastSaleAvgUSD    float64 `json:"last_sale_avg_usd"`
	LastSaleMedianUSD float64 `json:"last_sale_median_usd"`
	LastSalesCount    int     `json:"last_sales_count"`

	ExpectedSellUSD float64 `json:"expected_sell_usd"`
	GrossProfitUSD  float64 `json:"gross_profit_usd"`
	ROIPercent      float64 `json:"roi_percent"`

	Score  float64 `json:"score"`
	Risk   string  `json:"risk"`
	Reason string  `json:"reason"`

	SteamDT SteamDTFilterResult `json:"steamdt"`
}

type SteamDTMarketSignal struct {
	MarketHashName  string
	UniverseKnown   bool
	UniverseEnabled bool
	UniverseReason  string
	LatestFetchedAt time.Time
	PlatformCount   int
	TotalSellCount  int
	TotalBidCount   int
	SellSpikeAlerts int
	BidSpikeAlerts  int
}

type SteamDTFilterResult struct {
	Eligible           bool      `json:"eligible"`
	Reason             string    `json:"reason"`
	UniverseKnown      bool      `json:"universe_known"`
	UniverseEnabled    bool      `json:"universe_enabled"`
	UniverseReason     string    `json:"universe_reason,omitempty"`
	LatestFetchedAt    time.Time `json:"latest_fetched_at,omitempty"`
	SnapshotAgeSeconds int64     `json:"snapshot_age_seconds,omitempty"`
	PlatformCount      int       `json:"platform_count"`
	TotalSellCount     int       `json:"total_sell_count"`
	TotalBidCount      int       `json:"total_bid_count"`
	SellSpikeAlerts    int       `json:"sell_spike_alerts"`
	BidSpikeAlerts     int       `json:"bid_spike_alerts"`
}

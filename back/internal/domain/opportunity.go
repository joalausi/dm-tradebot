package domain

import "time"

type Opportunity struct {
	ScannedAt time.Time

	GameID string
	Title  string

	BestTargetUSD float64
	TargetCount   int

	BestOfferUSD float64
	OfferCount   int

	LastSaleAvgUSD    float64
	LastSaleMedianUSD float64
	LastSalesCount    int

	ExpectedSellUSD float64
	GrossProfitUSD  float64
	ROIPercent      float64

	Score  float64
	Risk   string
	Reason string
}

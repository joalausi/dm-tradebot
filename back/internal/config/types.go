package config

import "back/internal/domain"

type DiscordConfig struct {
	Mention string `yaml:"mention"`
}

type AccountTargetsConfig struct {
	Enabled     bool     `yaml:"enabled"`
	GameIDs     []string `yaml:"game_ids"`
	Statuses    []string `yaml:"statuses"`
	DefaultTopN int      `yaml:"default_top_n"`
}

type Config struct {
	PollEvery      string               `yaml:"poll_every"`
	Discord        DiscordConfig        `yaml:"discord"`
	AccountTargets AccountTargetsConfig `yaml:"account_targets"`
	Items          []domain.TargetItem  `yaml:"items"`
	DMarketCrawler DMarketCrawlerConfig `yaml:"dmarket_crawler"`
}

type DMarketCrawlerConfig struct {
	Enabled   bool   `yaml:"enabled"`
	PollEvery string `yaml:"poll_every"`

	GameID   string `yaml:"game_id"`
	Currency string `yaml:"currency"`

	PriceFromUSD float64 `yaml:"price_from_usd"`
	PriceToUSD   float64 `yaml:"price_to_usd"`

	Limit    int `yaml:"limit"`
	MaxPages int `yaml:"max_pages"`

	MinProfitUSD  float64 `yaml:"min_profit_usd"`
	MinROIPercent float64 `yaml:"min_roi_percent"`

	LastSalesLimit    int `yaml:"last_sales_limit"`
	MinLastSalesCount int `yaml:"min_last_sales_count"`

	SteamDTFilter SteamDTOpportunityFilterConfig `yaml:"steamdt_filter"`
}

type SteamDTOpportunityFilterConfig struct {
	Enabled bool `yaml:"enabled"`

	DatabasePath    string `yaml:"database_path"`
	MaxSnapshotAge  string `yaml:"max_snapshot_age"`
	AnomalyLookback string `yaml:"anomaly_lookback"`

	MinPlatforms      int `yaml:"min_platforms"`
	MinTotalSellCount int `yaml:"min_total_sell_count"`
	MinTotalBidCount  int `yaml:"min_total_bid_count"`

	AllowOutsideUniverse  bool `yaml:"allow_outside_universe"`
	AllowMissingSnapshots bool `yaml:"allow_missing_snapshots"`
}

package config

type SteamDTSmokeConfig struct {
	MarketHashName string             `yaml:"market_hash_name"`
	Watchlist      []SteamDTWatchItem `yaml:"watchlist"`

	Database struct {
		Path string `yaml:"path"`
	} `yaml:"database"`

	Signals SteamDTSignalConfig `yaml:"signals"`
}

type SteamDTWatchItem struct {
	MarketHashName string `yaml:"market_hash_name"`
	Enabled        bool   `yaml:"enabled"`
}

type SteamDTSignalConfig struct {
	LookbackDays       int `yaml:"lookback_days"`
	MinBaselineSamples int `yaml:"min_baseline_samples"`

	SellCountSpikePct float64 `yaml:"sell_count_spike_pct"`
	BidCountSpikePct  float64 `yaml:"bid_count_spike_pct"`

	MinCurrentSellCount int `yaml:"min_current_sell_count"`
	MinCurrentBidCount  int `yaml:"min_current_bid_count"`

	AlertCooldownHours int `yaml:"alert_cooldown_hours"`
}

package config

type SteamDTSmokeConfig struct {
	MarketHashName string             `yaml:"market_hash_name"`
	Watchlist      []SteamDTWatchItem `yaml:"watchlist"`

	Database struct {
		Path string `yaml:"path"`
	} `yaml:"database"`

	Catalog struct {
		SyncTTLHours int `yaml:"sync_ttl_hours"`
	} `yaml:"catalog"`

	Collector struct {
		BatchSize       int `yaml:"batch_size"`
		MaxChunksPerRun int `yaml:"max_chunks_per_run"`
	} `yaml:"collector"`

	Universe SteamDTUniverseConfig `yaml:"universe"`
	Signals  SteamDTSignalConfig   `yaml:"signals"`
}

type SteamDTUniverseConfig struct {
	IncludeSkins    bool `yaml:"include_skins"`
	IncludeAgents   bool `yaml:"include_agents"`
	IncludeStickers bool `yaml:"include_stickers"`

	IncludeStatTrak bool `yaml:"include_stattrak"`
	IncludeSouvenir bool `yaml:"include_souvenir"`

	RequirePlatforms []string `yaml:"require_platforms"`
	ExcludePatterns  []string `yaml:"exclude_patterns"`
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

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

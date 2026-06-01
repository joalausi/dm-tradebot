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
}

type SteamDTWatchItem struct {
	MarketHashName string `yaml:"market_hash_name"`
	Enabled        bool   `yaml:"enabled"`
}

package config

type SteamDTSmokeConfig struct {
	MarketHashName string             `yaml:"market_hash_name"`
	Watchlist      []SteamDTWatchItem `yaml:"watchlist"`
}

type SteamDTWatchItem struct {
	MarketHashName string `yaml:"market_hash_name"`
	Enabled        bool   `yaml:"enabled"`
}

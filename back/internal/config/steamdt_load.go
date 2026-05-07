package config

import (
	"fmt"
	"os"
	"strings"

	"gopkg.in/yaml.v3"
)

func LoadSteamDTSmokeConfig(path string) (SteamDTSmokeConfig, error) {
	var cfg SteamDTSmokeConfig

	data, err := os.ReadFile(path)
	if err != nil {
		return cfg, err
	}

	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return cfg, err
	}

	cfg.MarketHashName = strings.TrimSpace(cfg.MarketHashName)

	for i := range cfg.Watchlist {
		cfg.Watchlist[i].MarketHashName = strings.TrimSpace(cfg.Watchlist[i].MarketHashName)
	}

	if cfg.MarketHashName == "" && len(cfg.Watchlist) == 0 {
		return cfg, fmt.Errorf("either market_hash_name or watchlist is required")
	}

	return cfg, nil
}

func (c SteamDTSmokeConfig) ResolveWatchlist() []string {
	seen := make(map[string]struct{})
	var out []string

	if c.MarketHashName != "" {
		seen[c.MarketHashName] = struct{}{}
		out = append(out, c.MarketHashName)
	}

	for _, it := range c.Watchlist {
		if !it.Enabled {
			continue
		}
		if it.MarketHashName == "" {
			continue
		}
		if _, ok := seen[it.MarketHashName]; ok {
			continue
		}
		seen[it.MarketHashName] = struct{}{}
		out = append(out, it.MarketHashName)
	}

	return out
}

package config

import (
	"fmt"
	"os"
	"strings"

	"gopkg.in/yaml.v3"
)

func LoadSteamDTSmokeConfig(path string) (SteamDTSmokeConfig, error) {
	var cfg SteamDTSmokeConfig
	if strings.TrimSpace(cfg.Database.Path) == "" {
		cfg.Database.Path = "data/steamdt_smoke.db"
	}

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

	if strings.TrimSpace(cfg.Database.Path) == "" {
		cfg.Database.Path = "data/steamdt_smoke.db"
	}

	if cfg.Signals.LookbackDays <= 0 {
		cfg.Signals.LookbackDays = 3
	}
	if cfg.Signals.MinBaselineSamples <= 0 {
		cfg.Signals.MinBaselineSamples = 3
	}
	if cfg.Signals.SellCountSpikePct <= 0 {
		cfg.Signals.SellCountSpikePct = 50
	}
	if cfg.Signals.BidCountSpikePct <= 0 {
		cfg.Signals.BidCountSpikePct = 50
	}
	if cfg.Signals.MinCurrentSellCount <= 0 {
		cfg.Signals.MinCurrentSellCount = 20
	}
	if cfg.Signals.MinCurrentBidCount <= 0 {
		cfg.Signals.MinCurrentBidCount = 10
	}
	if cfg.Signals.AlertCooldownHours <= 0 {
		cfg.Signals.AlertCooldownHours = 24
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

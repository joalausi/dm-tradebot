package config

import (
	"fmt"
	"os"

	"gopkg.in/yaml.v3"
)

func LoadFromFile(path string) (Config, error) {
	var cfg Config

	data, err := os.ReadFile(path)
	if err != nil {
		return cfg, fmt.Errorf("read config %q: %w", path, err)
	}

	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return cfg, fmt.Errorf("parse config %q: %w", path, err)
	}

	applyDefaults(&cfg)

	return cfg, nil
}

func applyDefaults(cfg *Config) {
	if cfg.PollEvery == "" {
		cfg.PollEvery = "10m"
	}

	if cfg.DMarketCrawler.Currency == "" {
		cfg.DMarketCrawler.Currency = "USD"
	}

	if cfg.DMarketCrawler.GameID == "" {
		cfg.DMarketCrawler.GameID = "a8db"
	}

	if cfg.DMarketCrawler.Limit <= 0 {
		cfg.DMarketCrawler.Limit = 100
	}

	if cfg.DMarketCrawler.MaxPages <= 0 {
		cfg.DMarketCrawler.MaxPages = 3
	}

	if cfg.DMarketCrawler.LastSalesLimit <= 0 {
		cfg.DMarketCrawler.LastSalesLimit = 20
	}

	if cfg.DMarketCrawler.MinLastSalesCount <= 0 {
		cfg.DMarketCrawler.MinLastSalesCount = 3
	}

	for i := range cfg.Items {
		if cfg.Items[i].TopN <= 0 {
			cfg.Items[i].TopN = 5
		}
	}

	if cfg.AccountTargets.DefaultTopN <= 0 {
		cfg.AccountTargets.DefaultTopN = 5
	}

	if cfg.AccountTargets.Enabled {
		if len(cfg.AccountTargets.GameIDs) == 0 {
			cfg.AccountTargets.GameIDs = []string{"a8db"}
		}

		if len(cfg.AccountTargets.Statuses) == 0 {
			cfg.AccountTargets.Statuses = []string{
				"TargetStatusActive",
				"TargetStatusInactive",
			}
		}
	}

	cfg.AccountTargets.GameIDs = uniqueStrings(cfg.AccountTargets.GameIDs)
	cfg.AccountTargets.Statuses = uniqueStrings(cfg.AccountTargets.Statuses)
}

func uniqueStrings(in []string) []string {
	if len(in) == 0 {
		return nil
	}

	seen := make(map[string]struct{}, len(in))
	out := make([]string, 0, len(in))

	for _, v := range in {
		if v == "" {
			continue
		}
		if _, ok := seen[v]; ok {
			continue
		}
		seen[v] = struct{}{}
		out = append(out, v)
	}

	return out
}

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
	if cfg.MarketHashName == "" {
		return cfg, fmt.Errorf("market_hash_name is required")
	}

	return cfg, nil
}
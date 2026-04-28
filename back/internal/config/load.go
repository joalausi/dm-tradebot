package config

import (
	"os"

	"gopkg.in/yaml.v3"
)

func LoadFromFile(path string) (Config, error) {
	var cfg Config

	data, err := os.ReadFile(path)
	if err != nil {
		return cfg, err
	}

	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return cfg, err
	}

	if cfg.PollEvery == "" {
		cfg.PollEvery = "10m"
	}

	for i := range cfg.Items {
		if cfg.Items[i].TopN <= 0 {
			cfg.Items[i].TopN = 5
		}
	}

	return cfg, nil
}
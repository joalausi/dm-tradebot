package config

import "back/internal/domain"

type DiscordConfig struct {
	Mention string `yaml:"mention"`
}

type AccountTargetsConfig struct {
	Enabled     bool     `yaml:"enabled"`
	GameIDs     []string `yaml:"game_ids"`
	Statuses    []string `yaml:"statuses"`
	DefaultTopN int      `yaml:"default_top_n"`
}

type Config struct {
	PollEvery      string               `yaml:"poll_every"`
	Discord        DiscordConfig        `yaml:"discord"`
	AccountTargets AccountTargetsConfig `yaml:"account_targets"`
	Items          []domain.TargetItem  `yaml:"items"`
}

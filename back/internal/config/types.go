package config

import "back/internal/domain"

type DiscordConfig struct {
	WebhookURL string `yaml:"webhook_url"`
	Mention    string `yaml:"mention"`
}

type Config struct {
	PollEvery string              `yaml:"poll_every"`
	Discord   DiscordConfig       `yaml:"discord"`
	Items     []domain.TargetItem `yaml:"items"`
}

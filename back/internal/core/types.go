package core

import "context"

// type DMarketConfig struct {
//     PublicKeyEnv string `yaml:"public_key_env"` // env-переменная
//     SecretKeyEnv string `yaml:"secret_key_env"`
// }

type PriceLevel struct {
	Price float64 `json:"price"`
	Qty   int     `json:"qty"`
}

type Depth struct {
	Bids []PriceLevel `json:"bids"` // targets
	Asks []PriceLevel `json:"asks"` // offers
}

type Item struct {
	Title          string  `yaml:"title"`
	GameID         string  `yaml:"game_id"`
	MyTargetUSD    float64 `yaml:"my_target_usd"`
	LowAskAlertUSD float64 `yaml:"low_ask_alert_usd"`
	TopN           int     `yaml:"top_n"`
}

// Config лежит в core, чтобы runner знал про все настройки.
type DiscordConfig struct {
	WebhookURL string `yaml:"webhook_url"`
	BotToken   string `yaml:"bot_token"`
	ChannelID  string `yaml:"channel_id"`
	Mention    string `yaml:"mention"`
}

type Config struct {
	PollEvery string        `yaml:"poll_every"`
	Discord   DiscordConfig `yaml:"discord"`
	Items     []Item        `yaml:"items"`
}

// Порты (интерфейсы), на которые опирается core:

// MarketData можт отдавать стакан по тайтлу.
type MarketData interface {
	DepthByTitle(ctx context.Context, gameID, title string, topN int) (Depth, error)
}

// Notifier — любой способ отправить сообщение (Discord, консоль и т.п.)
type Notifier interface {
	Notify(ctx context.Context, msg string) error
}

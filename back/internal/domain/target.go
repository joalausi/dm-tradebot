package domain

type TargetItem struct {
	Title          string  `yaml:"title"`
	GameID         string  `yaml:"game_id"`
	MyTargetUSD    float64 `yaml:"my_target_usd"`
	LowAskAlertUSD float64 `yaml:"low_ask_alert_usd"`
	TopN           int     `yaml:"top_n"`
}
package domain

type TargetItem struct {
	Title          string  `yaml:"title" json:"title"`
	GameID         string  `yaml:"game_id" json:"game_id"`
	MyTargetUSD    float64 `yaml:"my_target_usd" json:"my_target_usd"`
	LowAskAlertUSD float64 `yaml:"low_ask_alert_usd" json:"low_ask_alert_usd"`
	TopN           int     `yaml:"top_n" json:"top_n"`

	// for user targets
	TargetID   string            `yaml:"-" json:"target_id,omitempty"`
	Status     string            `yaml:"-" json:"status,omitempty"`
	Amount     int               `yaml:"-" json:"amount,omitempty"`
	Attributes []TargetAttribute `yaml:"attributes,omitempty" json:"attributes,omitempty"`
}

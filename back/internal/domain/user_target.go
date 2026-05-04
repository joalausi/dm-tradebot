package domain

type TargetAttribute struct {
	Name  string `json:"name" yaml:"name"`
	Value string `json:"value" yaml:"value"`
}

type UserTarget struct {
	TargetID   string
	Title      string
	GameID     string
	Status     string
	PriceUSD   float64
	Amount     int
	Attributes []TargetAttribute
}

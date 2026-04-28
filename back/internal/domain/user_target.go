package domain

type TargetAttribute struct {
	Name  string
	Value string
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

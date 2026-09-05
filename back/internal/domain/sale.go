package domain

import "time"

type Sale struct {
	Title    string
	GameID   string
	PriceUSD float64
	SoldAt   time.Time
}

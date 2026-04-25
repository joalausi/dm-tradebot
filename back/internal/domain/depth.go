package domain

type PriceLevel struct {
	Price float64 `json:"price"`
	Qty   int     `json:"qty"`
}

type Depth struct {
	Bids []PriceLevel `json:"bids"`
	Asks []PriceLevel `json:"asks"`
}
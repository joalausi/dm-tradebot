package ports

import (
	"context"

	"back/internal/domain"
)

type MarketCatalog interface {
	ListMarketItems(ctx context.Context, req MarketItemsRequest) ([]domain.MarketItem, string, error)
	AggregatedPrices(ctx context.Context, gameID string, titles []string) ([]domain.Opportunity, error)
}

type MarketItemsRequest struct {
	GameID       string
	Currency     string
	PriceFromUSD float64
	PriceToUSD   float64
	Limit        int
	Cursor       string
}

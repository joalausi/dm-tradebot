package ports

import (
	"context"

	"back/internal/domain"
)

type MarketData interface {
	DepthByTitle(ctx context.Context, gameID, title string, topN int) (domain.Depth, error)
}
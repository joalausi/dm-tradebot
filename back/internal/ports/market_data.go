package ports

import (
	"context"

	"back/internal/domain"
)

type MarketData interface {
	DepthByTitle(ctx context.Context, gameID, title string, topN int) (domain.Depth, error)
	DepthByTarget(ctx context.Context, target domain.TargetItem, topN int) (domain.Depth, error)
}

// type AdvancedMarketData interface {
// 	DepthByTarget(ctx context.Context, target domain.TargetItem, topN int) (domain.Depth, error)
// }

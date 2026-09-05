package ports

import (
	"context"

	"back/internal/domain"
)

type OpportunityStore interface {
	SaveOpportunities(ctx context.Context, items []domain.Opportunity) error
}

type OpportunityProvider interface {
	CurrentOpportunities(ctx context.Context) ([]domain.Opportunity, error)
}

type SteamDTSignalReader interface {
	MarketSignal(
		ctx context.Context,
		marketHashName string,
		snapshotSinceUnix int64,
		anomalySinceUnix int64,
	) (domain.SteamDTMarketSignal, error)
}

type OpportunityFilter interface {
	Evaluate(ctx context.Context, marketHashName string) (domain.SteamDTFilterResult, error)
}

package ports

import (
	"context"

	"back/internal/domain"
)

type OpportunityStore interface {
	SaveOpportunities(ctx context.Context, items []domain.Opportunity) error
}

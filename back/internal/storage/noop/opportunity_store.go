package noop

import (
	"context"
	"fmt"

	"back/internal/domain"
	"back/internal/ports"
)

type OpportunityStore struct{}

func NewOpportunityStore() ports.OpportunityStore {
	return OpportunityStore{}
}

func (OpportunityStore) SaveOpportunities(ctx context.Context, items []domain.Opportunity) error {
	fmt.Printf("\n[noop opportunity store] received %d opportunities; DB save skipped\n", len(items))

	limit := len(items)
	if limit > 20 {
		limit = 20
	}

	for i := 0; i < limit; i++ {
		op := items[i]

		fmt.Printf(
			"[%02d] %s | target %.2f | offer %.2f | median %.2f | profit %.2f | ROI %.2f%% | score %.2f | risk=%s\n",
			i+1,
			op.Title,
			op.BestTargetUSD,
			op.BestOfferUSD,
			op.LastSaleMedianUSD,
			op.GrossProfitUSD,
			op.ROIPercent,
			op.Score,
			op.Risk,
		)
	}

	return nil
}

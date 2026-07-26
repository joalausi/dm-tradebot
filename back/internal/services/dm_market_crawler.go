package services

import (
	"context"
	"fmt"
	"sort"
	"time"

	"back/internal/config"
	"back/internal/domain"
	"back/internal/ports"
)

type DMarketMarketCrawler struct {
	cfg     config.DMarketCrawlerConfig
	catalog ports.MarketCatalog
	sales   ports.SalesHistory
	store   ports.OpportunityStore
}

func NewDMarketMarketCrawler(
	cfg config.DMarketCrawlerConfig,
	catalog ports.MarketCatalog,
	sales ports.SalesHistory,
	store ports.OpportunityStore,
) *DMarketMarketCrawler {
	return &DMarketMarketCrawler{
		cfg:     cfg,
		catalog: catalog,
		sales:   sales,
		store:   store,
	}
}

func (c *DMarketMarketCrawler) RunOnce(ctx context.Context) error {
	if !c.cfg.Enabled {
		return nil
	}

	cursor := ""
	allCandidates := make([]domain.Opportunity, 0)

	seenTitles := make(map[string]struct{})
	seenOpportunities := make(map[string]struct{})

	for page := 0; page < c.cfg.MaxPages; page++ {
		items, nextCursor, err := c.catalog.ListMarketItems(ctx, ports.MarketItemsRequest{
			GameID:       c.cfg.GameID,
			Currency:     c.cfg.Currency,
			PriceFromUSD: c.cfg.PriceFromUSD,
			PriceToUSD:   c.cfg.PriceToUSD,
			Limit:        c.cfg.Limit,
			Cursor:       cursor,
		})
		if err != nil {
			return fmt.Errorf("list market items page %d: %w", page, err)
		}

		if len(items) == 0 {
			break
		}

		titles := uniqueMarketTitles(items)
		titles = filterNewTitles(titles, seenTitles)

		if len(titles) == 0 {
			if nextCursor == "" || nextCursor == cursor {
				break
			}
			cursor = nextCursor
			continue
		}

		aggregated, err := c.catalog.AggregatedPrices(ctx, c.cfg.GameID, titles)
		if err != nil {
			return fmt.Errorf("aggregated prices page %d: %w", page, err)
		}

		for _, op := range aggregated {
			if op.BestTargetUSD <= 0 || op.BestOfferUSD <= 0 {
				continue
			}

			if !inUSDRange(op.BestOfferUSD, c.cfg.PriceFromUSD, c.cfg.PriceToUSD) {
				continue
			}

			roughProfit := op.BestOfferUSD - op.BestTargetUSD
			roughROI := roughProfit / op.BestTargetUSD * 100

			// Быстрый грубый фильтр, чтобы не дёргать last-sales по мусору.
			if roughProfit < c.cfg.MinProfitUSD || roughROI < c.cfg.MinROIPercent {
				continue
			}

			sales, err := c.sales.LastSales(ctx, c.cfg.GameID, op.Title, c.cfg.LastSalesLimit)
			if err != nil {
				fmt.Printf("[crawler] last sales error for %s: %v\n", op.Title, err)
				continue
			}

			if len(sales) < c.cfg.MinLastSalesCount {
				continue
			}

			avg, median := saleStats(sales)

			op.ScannedAt = time.Now()
			op.LastSaleAvgUSD = avg
			op.LastSaleMedianUSD = median
			op.LastSalesCount = len(sales)

			op.ExpectedSellUSD = minPositive(op.BestOfferUSD, op.LastSaleMedianUSD)
			op.GrossProfitUSD = op.ExpectedSellUSD - op.BestTargetUSD

			if op.BestTargetUSD > 0 {
				op.ROIPercent = op.GrossProfitUSD / op.BestTargetUSD * 100
			}

			op.Score, op.Risk, op.Reason = scoreOpportunity(op, c.cfg)

			if op.GrossProfitUSD < c.cfg.MinProfitUSD || op.ROIPercent < c.cfg.MinROIPercent {
				continue
			}

			opKey := op.GameID + "|" + op.Title
			if _, ok := seenOpportunities[opKey]; ok {
				continue
			}
			seenOpportunities[opKey] = struct{}{}

			allCandidates = append(allCandidates, op)
		}

		if nextCursor == "" || nextCursor == cursor {
			break
		}
		cursor = nextCursor
	}

	sort.Slice(allCandidates, func(i, j int) bool {
		return allCandidates[i].Score > allCandidates[j].Score
	})

	if c.store != nil && len(allCandidates) > 0 {
		if err := c.store.SaveOpportunities(ctx, allCandidates); err != nil {
			return fmt.Errorf("save opportunities: %w", err)
		}
	}

	printCrawlerResults(allCandidates)

	return nil
}

func uniqueMarketTitles(items []domain.MarketItem) []string {
	seen := make(map[string]struct{})
	out := make([]string, 0, len(items))

	for _, it := range items {
		if it.Title == "" {
			continue
		}
		if _, ok := seen[it.Title]; ok {
			continue
		}
		seen[it.Title] = struct{}{}
		out = append(out, it.Title)
	}

	return out
}

func filterNewTitles(titles []string, seen map[string]struct{}) []string {
	out := make([]string, 0, len(titles))

	for _, title := range titles {
		if title == "" {
			continue
		}

		if _, ok := seen[title]; ok {
			continue
		}

		seen[title] = struct{}{}
		out = append(out, title)
	}

	return out
}

func inUSDRange(value, from, to float64) bool {
	if value <= 0 {
		return false
	}

	if from > 0 && value < from {
		return false
	}

	if to > 0 && value > to {
		return false
	}

	return true
}

func saleStats(sales []domain.Sale) (avg, median float64) {
	if len(sales) == 0 {
		return 0, 0
	}

	values := make([]float64, 0, len(sales))
	sum := 0.0

	for _, s := range sales {
		if s.PriceUSD <= 0 {
			continue
		}
		values = append(values, s.PriceUSD)
		sum += s.PriceUSD
	}

	if len(values) == 0 {
		return 0, 0
	}

	sort.Float64s(values)

	avg = sum / float64(len(values))

	mid := len(values) / 2
	if len(values)%2 == 0 {
		median = (values[mid-1] + values[mid]) / 2
	} else {
		median = values[mid]
	}

	return avg, median
}

func minPositive(a, b float64) float64 {
	if a <= 0 {
		return b
	}
	if b <= 0 {
		return a
	}
	if a < b {
		return a
	}
	return b
}

func scoreOpportunity(op domain.Opportunity, cfg config.DMarketCrawlerConfig) (score float64, risk, reason string) {
	risk = "normal"

	score = op.ROIPercent
	score += float64(op.LastSalesCount) * 0.5

	if op.OfferCount <= 1 {
		score -= 5
		risk = "thin_offers"
	}

	if op.TargetCount > 100 {
		score -= 5
		risk = "crowded_targets"
	}

	reason = fmt.Sprintf(
		"roi=%.2f%% profit=%.2f sales=%d",
		op.ROIPercent,
		op.GrossProfitUSD,
		op.LastSalesCount,
	)

	return score, risk, reason
}

func printCrawlerResults(items []domain.Opportunity) {
	fmt.Println()
	fmt.Printf("========== DMarket Crawler: %d opportunities ==========\n", len(items))

	limit := len(items)
	if limit > 20 {
		limit = 20
	}

	for i := 0; i < limit; i++ {
		op := items[i]
		fmt.Printf(
			"[%02d] %s | target %.2f | offer %.2f | median %.2f | profit %.2f | ROI %.2f%% | score %.2f | %s\n",
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
}

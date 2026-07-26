package services

import (
	"context"
	"sort"
	"time"

	"back/internal/adapters/markets/steamdt"
	"back/internal/config"
	"back/internal/storage/sqlite"
)

type SteamDTAnomalyDetector struct {
	cfg  config.SteamDTSmokeConfig
	repo *sqlite.AnomalyRepository
}

func NewSteamDTAnomalyDetector(cfg config.SteamDTSmokeConfig, repo *sqlite.AnomalyRepository) *SteamDTAnomalyDetector {
	return &SteamDTAnomalyDetector{
		cfg:  cfg,
		repo: repo,
	}
}

func (d *SteamDTAnomalyDetector) Detect(
	ctx context.Context,
	results map[string]steamdt.PriceSingleResponse,
	fetchedAt time.Time,
) ([]sqlite.AnomalyAlert, error) {
	var alerts []sqlite.AnomalyAlert

	sinceUnix := fetchedAt.AddDate(0, 0, -d.cfg.Signals.LookbackDays).Unix()
	cooldownSinceUnix := fetchedAt.Add(-time.Duration(d.cfg.Signals.AlertCooldownHours) * time.Hour).Unix()

	for marketHashName, resp := range results {
		for _, p := range resp.Data {
			if isProbablyStaleForDetect(p) {
				continue
			}

			history, err := d.repo.GetHistory(ctx, marketHashName, p.Platform, sinceUnix, fetchedAt.Unix())
			if err != nil {
				return nil, err
			}

			if len(history) < d.cfg.Signals.MinBaselineSamples {
				continue
			}

			// sell_count_spike
			sellBaseline := medianInt(extractSellCounts(history))
			if sellBaseline > 0 {
				pct := pctChange(float64(p.SellCount), float64(sellBaseline))
				if p.SellCount >= d.cfg.Signals.MinCurrentSellCount &&
					pct >= d.cfg.Signals.SellCountSpikePct {

					exists, err := d.repo.HasRecentAlert(ctx, marketHashName, p.Platform, "sell_count_spike", cooldownSinceUnix)
					if err != nil {
						return nil, err
					}
					if !exists {
						alerts = append(alerts, sqlite.AnomalyAlert{
							MarketHashName: marketHashName,
							Platform:       p.Platform,
							Metric:         "sell_count_spike",
							CurrentValue:   float64(p.SellCount),
							BaselineValue:  float64(sellBaseline),
							PctChange:      pct,
							FetchedAtUnix:  fetchedAt.Unix(),
							CreatedAtUnix:  time.Now().Unix(),
						})
					}
				}
			}

			// bid_count_spike
			bidBaseline := medianInt(extractBidCounts(history))
			if bidBaseline > 0 {
				pct := pctChange(float64(p.BiddingCount), float64(bidBaseline))
				if p.BiddingCount >= d.cfg.Signals.MinCurrentBidCount &&
					pct >= d.cfg.Signals.BidCountSpikePct {

					exists, err := d.repo.HasRecentAlert(ctx, marketHashName, p.Platform, "bid_count_spike", cooldownSinceUnix)
					if err != nil {
						return nil, err
					}
					if !exists {
						alerts = append(alerts, sqlite.AnomalyAlert{
							MarketHashName: marketHashName,
							Platform:       p.Platform,
							Metric:         "bid_count_spike",
							CurrentValue:   float64(p.BiddingCount),
							BaselineValue:  float64(bidBaseline),
							PctChange:      pct,
							FetchedAtUnix:  fetchedAt.Unix(),
							CreatedAtUnix:  time.Now().Unix(),
						})
					}
				}
			}
		}
	}

	return alerts, nil
}

func extractSellCounts(history []sqlite.HistoryPoint) []int {
	out := make([]int, 0, len(history))
	for _, h := range history {
		out = append(out, h.SellCount)
	}
	return out
}

func extractBidCounts(history []sqlite.HistoryPoint) []int {
	out := make([]int, 0, len(history))
	for _, h := range history {
		out = append(out, h.BiddingCount)
	}
	return out
}

func medianInt(values []int) int {
	if len(values) == 0 {
		return 0
	}

	cp := append([]int(nil), values...)
	sort.Ints(cp)

	n := len(cp)
	mid := n / 2
	if n%2 == 1 {
		return cp[mid]
	}
	return (cp[mid-1] + cp[mid]) / 2
}

func pctChange(current float64, baseline float64) float64 {
	if baseline <= 0 {
		return 0
	}
	return ((current - baseline) / baseline) * 100
}

func isProbablyStaleForDetect(p steamdt.PlatformPrice) bool {
	return p.SellPrice == 0 &&
		p.SellCount == 0 &&
		p.BiddingPrice == 0 &&
		p.BiddingCount == 0
}

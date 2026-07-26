package sqlite

import (
	"context"
	"database/sql"
	"time"

	"back/internal/domain"
)

type OpportunityRepository struct {
	db *sql.DB
}

func NewOpportunityRepository(db *sql.DB) *OpportunityRepository {
	return &OpportunityRepository{db: db}
}

func (r *OpportunityRepository) SaveOpportunities(ctx context.Context, items []domain.Opportunity) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.ExecContext(ctx, `delete from dmarket_opportunities`); err != nil {
		return err
	}

	stmt, err := tx.PrepareContext(ctx, `
		insert into dmarket_opportunities (
			game_id, title, scanned_at_unix,
			best_target_usd, target_count, best_offer_usd, offer_count,
			last_sale_avg_usd, last_sale_median_usd, last_sales_count,
			expected_sell_usd, gross_profit_usd, roi_percent,
			score, risk, reason,
			steamdt_eligible, steamdt_reason,
			steamdt_universe_known, steamdt_universe_enabled, steamdt_universe_reason,
			steamdt_latest_fetched_at_unix, steamdt_snapshot_age_seconds,
			steamdt_platform_count, steamdt_total_sell_count, steamdt_total_bid_count,
			steamdt_sell_spike_alerts, steamdt_bid_spike_alerts
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
	`)
	if err != nil {
		return err
	}
	defer stmt.Close()

	for _, item := range items {
		latestFetchedAtUnix := int64(0)
		if !item.SteamDT.LatestFetchedAt.IsZero() {
			latestFetchedAtUnix = item.SteamDT.LatestFetchedAt.Unix()
		}

		if _, err := stmt.ExecContext(
			ctx,
			item.GameID,
			item.Title,
			item.ScannedAt.Unix(),
			item.BestTargetUSD,
			item.TargetCount,
			item.BestOfferUSD,
			item.OfferCount,
			item.LastSaleAvgUSD,
			item.LastSaleMedianUSD,
			item.LastSalesCount,
			item.ExpectedSellUSD,
			item.GrossProfitUSD,
			item.ROIPercent,
			item.Score,
			item.Risk,
			item.Reason,
			boolInt(item.SteamDT.Eligible),
			item.SteamDT.Reason,
			boolInt(item.SteamDT.UniverseKnown),
			boolInt(item.SteamDT.UniverseEnabled),
			item.SteamDT.UniverseReason,
			latestFetchedAtUnix,
			item.SteamDT.SnapshotAgeSeconds,
			item.SteamDT.PlatformCount,
			item.SteamDT.TotalSellCount,
			item.SteamDT.TotalBidCount,
			item.SteamDT.SellSpikeAlerts,
			item.SteamDT.BidSpikeAlerts,
		); err != nil {
			return err
		}
	}

	return tx.Commit()
}

func (r *OpportunityRepository) CurrentOpportunities(ctx context.Context) ([]domain.Opportunity, error) {
	rows, err := r.db.QueryContext(ctx, `
		select
			game_id, title, scanned_at_unix,
			best_target_usd, target_count, best_offer_usd, offer_count,
			last_sale_avg_usd, last_sale_median_usd, last_sales_count,
			expected_sell_usd, gross_profit_usd, roi_percent,
			score, risk, reason,
			steamdt_eligible, steamdt_reason,
			steamdt_universe_known, steamdt_universe_enabled, steamdt_universe_reason,
			steamdt_latest_fetched_at_unix, steamdt_snapshot_age_seconds,
			steamdt_platform_count, steamdt_total_sell_count, steamdt_total_bid_count,
			steamdt_sell_spike_alerts, steamdt_bid_spike_alerts
		from dmarket_opportunities
		order by score desc, scanned_at_unix desc, title asc
	`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []domain.Opportunity
	for rows.Next() {
		var item domain.Opportunity
		var scannedAtUnix int64
		var latestFetchedAtUnix int64
		var steamDTEligible int
		var universeKnown int
		var universeEnabled int

		if err := rows.Scan(
			&item.GameID,
			&item.Title,
			&scannedAtUnix,
			&item.BestTargetUSD,
			&item.TargetCount,
			&item.BestOfferUSD,
			&item.OfferCount,
			&item.LastSaleAvgUSD,
			&item.LastSaleMedianUSD,
			&item.LastSalesCount,
			&item.ExpectedSellUSD,
			&item.GrossProfitUSD,
			&item.ROIPercent,
			&item.Score,
			&item.Risk,
			&item.Reason,
			&steamDTEligible,
			&item.SteamDT.Reason,
			&universeKnown,
			&universeEnabled,
			&item.SteamDT.UniverseReason,
			&latestFetchedAtUnix,
			&item.SteamDT.SnapshotAgeSeconds,
			&item.SteamDT.PlatformCount,
			&item.SteamDT.TotalSellCount,
			&item.SteamDT.TotalBidCount,
			&item.SteamDT.SellSpikeAlerts,
			&item.SteamDT.BidSpikeAlerts,
		); err != nil {
			return nil, err
		}

		item.ScannedAt = time.Unix(scannedAtUnix, 0).UTC()
		item.SteamDT.Eligible = steamDTEligible == 1
		item.SteamDT.UniverseKnown = universeKnown == 1
		item.SteamDT.UniverseEnabled = universeEnabled == 1
		if latestFetchedAtUnix > 0 {
			item.SteamDT.LatestFetchedAt = time.Unix(latestFetchedAtUnix, 0).UTC()
		}

		out = append(out, item)
	}

	return out, rows.Err()
}

func boolInt(value bool) int {
	if value {
		return 1
	}
	return 0
}

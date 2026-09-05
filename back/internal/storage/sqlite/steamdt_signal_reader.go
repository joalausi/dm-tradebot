package sqlite

import (
	"context"
	"database/sql"
	"time"

	"back/internal/domain"
)

type SteamDTSignalReader struct {
	db *sql.DB
}

func NewSteamDTSignalReader(db *sql.DB) *SteamDTSignalReader {
	return &SteamDTSignalReader{db: db}
}

func (r *SteamDTSignalReader) MarketSignal(
	ctx context.Context,
	marketHashName string,
	snapshotSinceUnix int64,
	anomalySinceUnix int64,
) (domain.SteamDTMarketSignal, error) {
	signal := domain.SteamDTMarketSignal{MarketHashName: marketHashName}

	var enabled int
	err := r.db.QueryRowContext(ctx, `
		select enabled, reason
		from steamdt_working_universe
		where market_hash_name = ?
	`, marketHashName).Scan(&enabled, &signal.UniverseReason)
	switch {
	case err == nil:
		signal.UniverseKnown = true
		signal.UniverseEnabled = enabled == 1
	case err == sql.ErrNoRows:
	case err != nil:
		return signal, err
	}

	var latestFetchedAtUnix int64
	err = r.db.QueryRowContext(ctx, `
		select
			count(1),
			coalesce(sum(sell_count), 0),
			coalesce(sum(bidding_count), 0),
			coalesce(max(fetched_at_unix), 0)
		from (
			select
				sell_count,
				bidding_count,
				fetched_at_unix,
				row_number() over (
					partition by platform
					order by fetched_at_unix desc, id desc
				) as snapshot_rank
			from steamdt_market_snapshots
			where market_hash_name = ?
			  and is_stale = 0
			  and fetched_at_unix >= ?
		)
		where snapshot_rank = 1
	`, marketHashName, snapshotSinceUnix).Scan(
		&signal.PlatformCount,
		&signal.TotalSellCount,
		&signal.TotalBidCount,
		&latestFetchedAtUnix,
	)
	if err != nil {
		return signal, err
	}
	if latestFetchedAtUnix > 0 {
		signal.LatestFetchedAt = time.Unix(latestFetchedAtUnix, 0).UTC()
	}

	err = r.db.QueryRowContext(ctx, `
		select
			coalesce(sum(case when metric = 'sell_count_spike' then 1 else 0 end), 0),
			coalesce(sum(case when metric = 'bid_count_spike' then 1 else 0 end), 0)
		from steamdt_anomaly_alerts
		where market_hash_name = ?
		  and created_at_unix >= ?
	`, marketHashName, anomalySinceUnix).Scan(
		&signal.SellSpikeAlerts,
		&signal.BidSpikeAlerts,
	)
	if err != nil {
		return signal, err
	}

	return signal, nil
}

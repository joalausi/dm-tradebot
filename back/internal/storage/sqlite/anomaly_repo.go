package sqlite

import (
	"context"
	"database/sql"
)

type HistoryPoint struct {
	SellCount      int
	BiddingCount   int
	FetchedAtUnix  int64
	UpdateTimeUnix int64
}

type AnomalyAlert struct {
	MarketHashName string
	Platform       string
	Metric         string
	CurrentValue   float64
	BaselineValue  float64
	PctChange      float64
	FetchedAtUnix  int64
	CreatedAtUnix  int64
}

type AnomalyRepository struct {
	db *sql.DB
}

func NewAnomalyRepository(db *sql.DB) *AnomalyRepository {
	return &AnomalyRepository{db: db}
}

func (r *AnomalyRepository) GetHistory(
	ctx context.Context,
	marketHashName string,
	platform string,
	sinceUnix int64,
	beforeFetchedAtUnix int64,
) ([]HistoryPoint, error) {
	rows, err := r.db.QueryContext(ctx, `
		select sell_count, bidding_count, fetched_at_unix, update_time_unix
		from steamdt_market_snapshots
		where market_hash_name = ?
		  and platform = ?
		  and is_stale = 0
		  and fetched_at_unix >= ?
		  and fetched_at_unix < ?
		order by fetched_at_unix asc
	`, marketHashName, platform, sinceUnix, beforeFetchedAtUnix)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []HistoryPoint
	for rows.Next() {
		var p HistoryPoint
		if err := rows.Scan(&p.SellCount, &p.BiddingCount, &p.FetchedAtUnix, &p.UpdateTimeUnix); err != nil {
			return nil, err
		}
		out = append(out, p)
	}

	return out, rows.Err()
}

func (r *AnomalyRepository) HasRecentAlert(
	ctx context.Context,
	marketHashName string,
	platform string,
	metric string,
	sinceUnix int64,
) (bool, error) {
	var count int
	err := r.db.QueryRowContext(ctx, `
		select count(1)
		from steamdt_anomaly_alerts
		where market_hash_name = ?
		  and platform = ?
		  and metric = ?
		  and created_at_unix >= ?
	`, marketHashName, platform, metric, sinceUnix).Scan(&count)
	if err != nil {
		return false, err
	}
	return count > 0, nil
}

func (r *AnomalyRepository) SaveAlerts(ctx context.Context, alerts []AnomalyAlert) error {
	if len(alerts) == 0 {
		return nil
	}

	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()

	stmt, err := tx.PrepareContext(ctx, `
		insert into steamdt_anomaly_alerts (
			market_hash_name,
			platform,
			metric,
			current_value,
			baseline_value,
			pct_change,
			fetched_at_unix,
			created_at_unix
		) values (?, ?, ?, ?, ?, ?, ?, ?)
	`)
	if err != nil {
		return err
	}
	defer stmt.Close()

	for _, a := range alerts {
		if _, err := stmt.ExecContext(
			ctx,
			a.MarketHashName,
			a.Platform,
			a.Metric,
			a.CurrentValue,
			a.BaselineValue,
			a.PctChange,
			a.FetchedAtUnix,
			a.CreatedAtUnix,
		); err != nil {
			return err
		}
	}

	return tx.Commit()
}

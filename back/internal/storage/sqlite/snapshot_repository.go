package sqlite

import (
	"context"
	"database/sql"
	"time"

	"back/internal/adapters/markets/steamdt"
)

type SnapshotRepository struct {
	db *sql.DB
}

func NewSnapshotRepository(db *sql.DB) *SnapshotRepository {
	return &SnapshotRepository{db: db}
}

func (r *SnapshotRepository) SaveBatch(
	ctx context.Context,
	results map[string]steamdt.PriceSingleResponse,
	fetchedAt time.Time,
) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()

	stmt, err := tx.PrepareContext(ctx, `
		insert into steamdt_market_snapshots (
			market_hash_name,
			platform,
			platform_item_id,
			sell_price,
			sell_count,
			bidding_price,
			bidding_count,
			update_time_unix,
			fetched_at_unix,
			is_stale
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
	`)
	if err != nil {
		return err
	}
	defer stmt.Close()

	for marketHashName, resp := range results {
		for _, p := range resp.Data {
			isStale := 0
			if isProbablyStale(p, fetchedAt) {
				isStale = 1
			}

			if _, err := stmt.ExecContext(
				ctx,
				marketHashName,
				p.Platform,
				p.PlatformItemID,
				p.SellPrice,
				p.SellCount,
				p.BiddingPrice,
				p.BiddingCount,
				p.UpdateTime,
				fetchedAt.Unix(),
				isStale,
			); err != nil {
				return err
			}
		}
	}

	return tx.Commit()
}

func isProbablyStale(p steamdt.PlatformPrice, fetchedAt time.Time) bool {
	if p.SellPrice == 0 &&
		p.SellCount == 0 &&
		p.BiddingPrice == 0 &&
		p.BiddingCount == 0 {
		return true
	}

	if p.UpdateTime <= 0 {
		return true
	}

	updatedAt := time.Unix(p.UpdateTime, 0)
	if fetchedAt.Sub(updatedAt) > 7*24*time.Hour {
		return true
	}

	return false
}
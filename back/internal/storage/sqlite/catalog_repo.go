package sqlite

import (
	"context"
	"database/sql"
	"encoding/json"
	"time"

	"back/internal/adapters/markets/steamdt"
)

type CatalogItem struct {
	MarketHashName string
	Name           string
	RawJSON        string
	SyncedAtUnix   int64
}

type CatalogRepository struct {
	db *sql.DB
}

func NewCatalogRepository(db *sql.DB) *CatalogRepository {
	return &CatalogRepository{db: db}
}

func (r *CatalogRepository) ReplaceAll(
	ctx context.Context,
	items []steamdt.BaseItem,
	syncedAt time.Time,
) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()

	if _, err := tx.ExecContext(ctx, `delete from steamdt_catalog_items`); err != nil {
		return err
	}

	stmt, err := tx.PrepareContext(ctx, `
		insert into steamdt_catalog_items (
			market_hash_name,
			name,
			raw_json,
			synced_at_unix
		) values (?, ?, ?, ?)
	`)
	if err != nil {
		return err
	}
	defer stmt.Close()

	for _, item := range items {
		raw, err := json.Marshal(item)
		if err != nil {
			return err
		}

		if _, err := stmt.ExecContext(
			ctx,
			item.MarketHashName,
			item.Name,
			string(raw),
			syncedAt.Unix(),
		); err != nil {
			return err
		}
	}

	return tx.Commit()
}

func (r *CatalogRepository) Count(ctx context.Context) (int, error) {
	var n int
	err := r.db.QueryRowContext(ctx, `select count(1) from steamdt_catalog_items`).Scan(&n)
	return n, err
}

func (r *CatalogRepository) ListChunk(
	ctx context.Context,
	limit int,
	offset int,
) ([]CatalogItem, error) {
	rows, err := r.db.QueryContext(ctx, `
		select market_hash_name, name, raw_json, synced_at_unix
		from steamdt_catalog_items
		order by market_hash_name asc
		limit ? offset ?
	`, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []CatalogItem
	for rows.Next() {
		var item CatalogItem
		if err := rows.Scan(&item.MarketHashName, &item.Name, &item.RawJSON, &item.SyncedAtUnix); err != nil {
			return nil, err
		}
		out = append(out, item)
	}

	return out, rows.Err()
}

func (r *CatalogRepository) LastSyncedAt(ctx context.Context) (int64, error) {
	var ts sql.NullInt64
	err := r.db.QueryRowContext(ctx, `select max(synced_at_unix) from steamdt_catalog_items`).Scan(&ts)
	if err != nil {
		return 0, err
	}
	if !ts.Valid {
		return 0, nil
	}
	return ts.Int64, nil
}
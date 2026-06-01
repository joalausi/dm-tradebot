package sqlite

import (
	"context"
	"database/sql"
	"strconv"
	"time"
)

type CollectorStateRepository struct {
	db *sql.DB
}

func NewCollectorStateRepository(db *sql.DB) *CollectorStateRepository {
	return &CollectorStateRepository{db: db}
}

func (r *CollectorStateRepository) GetInt(ctx context.Context, key string, defaultValue int) (int, error) {
	var raw sql.NullString
	err := r.db.QueryRowContext(ctx, `
		select state_value
		from steamdt_collector_state
		where state_key = ?
	`, key).Scan(&raw)
	if err == sql.ErrNoRows {
		return defaultValue, nil
	}
	if err != nil {
		return 0, err
	}
	if !raw.Valid {
		return defaultValue, nil
	}

	v, err := strconv.Atoi(raw.String)
	if err != nil {
		return defaultValue, nil
	}
	return v, nil
}

func (r *CollectorStateRepository) SetInt(ctx context.Context, key string, value int) error {
	_, err := r.db.ExecContext(ctx, `
		insert into steamdt_collector_state (state_key, state_value, updated_at_unix)
		values (?, ?, ?)
		on conflict(state_key) do update set
			state_value = excluded.state_value,
			updated_at_unix = excluded.updated_at_unix
	`, key, strconv.Itoa(value), time.Now().Unix())

	return err
}
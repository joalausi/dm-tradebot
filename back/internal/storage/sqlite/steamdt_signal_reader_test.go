package sqlite

import (
	"context"
	"path/filepath"
	"testing"
	"time"
)

func TestSteamDTSignalReaderUsesLatestFreshSnapshotPerPlatform(t *testing.T) {
	ctx := context.Background()
	db, err := Open(filepath.Join(t.TempDir(), "test.db"))
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	if err := Migrate(ctx, db); err != nil {
		t.Fatal(err)
	}

	const title = "AK-47 | Test"
	if _, err := db.ExecContext(ctx, `
		insert into steamdt_working_universe (
			market_hash_name, enabled, reason, updated_at_unix
		) values (?, 1, 'eligible', 1000)
	`, title); err != nil {
		t.Fatal(err)
	}

	for _, snapshot := range []struct {
		platform  string
		sells     int
		bids      int
		fetchedAt int64
		stale     int
	}{
		{platform: "BUFF", sells: 10, bids: 5, fetchedAt: 100},
		{platform: "BUFF", sells: 20, bids: 8, fetchedAt: 200},
		{platform: "STEAM", sells: 30, bids: 12, fetchedAt: 150},
		{platform: "C5", sells: 999, bids: 999, fetchedAt: 250, stale: 1},
	} {
		if _, err := db.ExecContext(ctx, `
			insert into steamdt_market_snapshots (
				market_hash_name, platform, sell_count, bidding_count,
				fetched_at_unix, is_stale
			) values (?, ?, ?, ?, ?, ?)
		`, title, snapshot.platform, snapshot.sells, snapshot.bids, snapshot.fetchedAt, snapshot.stale); err != nil {
			t.Fatal(err)
		}
	}

	if _, err := db.ExecContext(ctx, `
		insert into steamdt_anomaly_alerts (
			market_hash_name, platform, metric, current_value, baseline_value,
			pct_change, fetched_at_unix, created_at_unix
		) values
			(?, 'BUFF', 'sell_count_spike', 20, 10, 100, 200, 200),
			(?, 'STEAM', 'bid_count_spike', 12, 5, 140, 150, 150),
			(?, 'BUFF', 'bid_count_spike', 8, 4, 100, 90, 90)
	`, title, title, title); err != nil {
		t.Fatal(err)
	}

	signal, err := NewSteamDTSignalReader(db).MarketSignal(ctx, title, 100, 100)
	if err != nil {
		t.Fatal(err)
	}

	if !signal.UniverseKnown || !signal.UniverseEnabled || signal.UniverseReason != "eligible" {
		t.Fatalf("unexpected universe signal: %#v", signal)
	}
	if signal.PlatformCount != 2 || signal.TotalSellCount != 50 || signal.TotalBidCount != 20 {
		t.Fatalf("unexpected liquidity aggregation: %#v", signal)
	}
	if !signal.LatestFetchedAt.Equal(time.Unix(200, 0).UTC()) {
		t.Fatalf("latest fetched at=%s, want=%s", signal.LatestFetchedAt, time.Unix(200, 0).UTC())
	}
	if signal.SellSpikeAlerts != 1 || signal.BidSpikeAlerts != 1 {
		t.Fatalf("unexpected anomaly counts: %#v", signal)
	}
}

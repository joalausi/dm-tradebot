package services

import (
	"context"
	"errors"
	"testing"
	"time"

	"back/internal/config"
	"back/internal/domain"
)

type stubSteamDTSignalReader struct {
	signal domain.SteamDTMarketSignal
	err    error
}

func (s stubSteamDTSignalReader) MarketSignal(
	context.Context,
	string,
	int64,
	int64,
) (domain.SteamDTMarketSignal, error) {
	return s.signal, s.err
}

func TestSteamDTOpportunityFilter(t *testing.T) {
	now := time.Date(2026, 7, 26, 12, 0, 0, 0, time.UTC)
	baseSignal := domain.SteamDTMarketSignal{
		UniverseKnown:   true,
		UniverseEnabled: true,
		LatestFetchedAt: now.Add(-time.Hour),
		PlatformCount:   5,
		TotalSellCount:  100,
		TotalBidCount:   50,
	}

	tests := []struct {
		name   string
		signal domain.SteamDTMarketSignal
		want   string
		ok     bool
	}{
		{name: "eligible", signal: baseSignal, want: "steamdt_liquidity_ok", ok: true},
		{name: "unknown universe", signal: domain.SteamDTMarketSignal{}, want: "steamdt_universe_unknown"},
		{name: "disabled universe", signal: withSignal(baseSignal, func(s *domain.SteamDTMarketSignal) { s.UniverseEnabled = false }), want: "steamdt_universe_disabled"},
		{name: "missing snapshots", signal: withSignal(baseSignal, func(s *domain.SteamDTMarketSignal) { s.PlatformCount = 0 }), want: "steamdt_snapshots_missing"},
		{name: "few platforms", signal: withSignal(baseSignal, func(s *domain.SteamDTMarketSignal) { s.PlatformCount = 2 }), want: "steamdt_platform_liquidity_too_low"},
		{name: "low sells", signal: withSignal(baseSignal, func(s *domain.SteamDTMarketSignal) { s.TotalSellCount = 19 }), want: "steamdt_sell_liquidity_too_low"},
		{name: "low bids", signal: withSignal(baseSignal, func(s *domain.SteamDTMarketSignal) { s.TotalBidCount = 9 }), want: "steamdt_bid_liquidity_too_low"},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			filter, err := NewSteamDTOpportunityFilter(testSteamDTFilterConfig(), stubSteamDTSignalReader{signal: tt.signal})
			if err != nil {
				t.Fatal(err)
			}
			filter.now = func() time.Time { return now }

			got, err := filter.Evaluate(context.Background(), "AK-47 | Test")
			if err != nil {
				t.Fatal(err)
			}
			if got.Eligible != tt.ok || got.Reason != tt.want {
				t.Fatalf("got eligible=%v reason=%q, want eligible=%v reason=%q", got.Eligible, got.Reason, tt.ok, tt.want)
			}
		})
	}
}

func TestSteamDTOpportunityFilterFailsClosedOnReaderError(t *testing.T) {
	filter, err := NewSteamDTOpportunityFilter(
		testSteamDTFilterConfig(),
		stubSteamDTSignalReader{err: errors.New("database locked")},
	)
	if err != nil {
		t.Fatal(err)
	}

	if _, err := filter.Evaluate(context.Background(), "AK-47 | Test"); err == nil {
		t.Fatal("expected reader error")
	}
}

func testSteamDTFilterConfig() config.SteamDTOpportunityFilterConfig {
	return config.SteamDTOpportunityFilterConfig{
		Enabled:           true,
		MaxSnapshotAge:    "36h",
		AnomalyLookback:   "24h",
		MinPlatforms:      3,
		MinTotalSellCount: 20,
		MinTotalBidCount:  10,
	}
}

func withSignal(
	signal domain.SteamDTMarketSignal,
	modify func(*domain.SteamDTMarketSignal),
) domain.SteamDTMarketSignal {
	modify(&signal)
	return signal
}

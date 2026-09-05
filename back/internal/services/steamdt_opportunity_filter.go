package services

import (
	"context"
	"fmt"
	"time"

	"back/internal/config"
	"back/internal/domain"
	"back/internal/ports"
)

type SteamDTOpportunityFilter struct {
	cfg             config.SteamDTOpportunityFilterConfig
	reader          ports.SteamDTSignalReader
	maxSnapshotAge  time.Duration
	anomalyLookback time.Duration
	now             func() time.Time
}

func NewSteamDTOpportunityFilter(
	cfg config.SteamDTOpportunityFilterConfig,
	reader ports.SteamDTSignalReader,
) (*SteamDTOpportunityFilter, error) {
	if reader == nil {
		return nil, fmt.Errorf("steamdt signal reader is nil")
	}

	maxSnapshotAge, err := time.ParseDuration(cfg.MaxSnapshotAge)
	if err != nil || maxSnapshotAge <= 0 {
		return nil, fmt.Errorf("invalid SteamDT max_snapshot_age %q", cfg.MaxSnapshotAge)
	}

	anomalyLookback, err := time.ParseDuration(cfg.AnomalyLookback)
	if err != nil || anomalyLookback <= 0 {
		return nil, fmt.Errorf("invalid SteamDT anomaly_lookback %q", cfg.AnomalyLookback)
	}

	return &SteamDTOpportunityFilter{
		cfg:             cfg,
		reader:          reader,
		maxSnapshotAge:  maxSnapshotAge,
		anomalyLookback: anomalyLookback,
		now:             time.Now,
	}, nil
}

func (f *SteamDTOpportunityFilter) Evaluate(
	ctx context.Context,
	marketHashName string,
) (domain.SteamDTFilterResult, error) {
	if !f.cfg.Enabled {
		return domain.SteamDTFilterResult{
			Eligible: true,
			Reason:   "steamdt_filter_disabled",
		}, nil
	}

	now := f.now().UTC()
	signal, err := f.reader.MarketSignal(
		ctx,
		marketHashName,
		now.Add(-f.maxSnapshotAge).Unix(),
		now.Add(-f.anomalyLookback).Unix(),
	)
	if err != nil {
		return domain.SteamDTFilterResult{}, fmt.Errorf("read SteamDT signal for %q: %w", marketHashName, err)
	}

	result := domain.SteamDTFilterResult{
		UniverseKnown:   signal.UniverseKnown,
		UniverseEnabled: signal.UniverseEnabled,
		UniverseReason:  signal.UniverseReason,
		LatestFetchedAt: signal.LatestFetchedAt,
		PlatformCount:   signal.PlatformCount,
		TotalSellCount:  signal.TotalSellCount,
		TotalBidCount:   signal.TotalBidCount,
		SellSpikeAlerts: signal.SellSpikeAlerts,
		BidSpikeAlerts:  signal.BidSpikeAlerts,
	}
	if !signal.LatestFetchedAt.IsZero() {
		age := now.Sub(signal.LatestFetchedAt)
		if age < 0 {
			age = 0
		}
		result.SnapshotAgeSeconds = int64(age.Seconds())
	}

	if !f.cfg.AllowOutsideUniverse {
		switch {
		case !signal.UniverseKnown:
			result.Reason = "steamdt_universe_unknown"
			return result, nil
		case !signal.UniverseEnabled:
			result.Reason = "steamdt_universe_disabled"
			return result, nil
		}
	}

	if signal.PlatformCount == 0 {
		if f.cfg.AllowMissingSnapshots {
			result.Eligible = true
			result.Reason = "steamdt_snapshots_missing_allowed"
			return result, nil
		}
		result.Reason = "steamdt_snapshots_missing"
		return result, nil
	}

	switch {
	case signal.PlatformCount < f.cfg.MinPlatforms:
		result.Reason = "steamdt_platform_liquidity_too_low"
	case signal.TotalSellCount < f.cfg.MinTotalSellCount:
		result.Reason = "steamdt_sell_liquidity_too_low"
	case signal.TotalBidCount < f.cfg.MinTotalBidCount:
		result.Reason = "steamdt_bid_liquidity_too_low"
	default:
		result.Eligible = true
		result.Reason = "steamdt_liquidity_ok"
	}

	return result, nil
}

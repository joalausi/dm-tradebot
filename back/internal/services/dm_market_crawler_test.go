package services

import (
	"context"
	"testing"
	"time"

	"back/internal/config"
	"back/internal/domain"
	"back/internal/ports"
)

type crawlerCatalogStub struct{}

func (crawlerCatalogStub) ListMarketItems(context.Context, ports.MarketItemsRequest) ([]domain.MarketItem, string, error) {
	return []domain.MarketItem{{Title: "AK-47 | Test", GameID: "a8db", BestOfferUSD: 12}}, "", nil
}

func (crawlerCatalogStub) AggregatedPrices(context.Context, string, []string) ([]domain.Opportunity, error) {
	return []domain.Opportunity{{
		GameID:        "a8db",
		Title:         "AK-47 | Test",
		BestTargetUSD: 10,
		BestOfferUSD:  12,
		TargetCount:   20,
		OfferCount:    10,
	}}, nil
}

type crawlerSalesStub struct{ calls int }

func (s *crawlerSalesStub) LastSales(context.Context, string, string, int) ([]domain.Sale, error) {
	s.calls++
	return []domain.Sale{{PriceUSD: 12, SoldAt: time.Now()}}, nil
}

type crawlerStoreStub struct {
	calls int
	items []domain.Opportunity
}

func (s *crawlerStoreStub) SaveOpportunities(_ context.Context, items []domain.Opportunity) error {
	s.calls++
	s.items = append([]domain.Opportunity(nil), items...)
	return nil
}

type crawlerFilterStub struct{ result domain.SteamDTFilterResult }

func (s crawlerFilterStub) Evaluate(context.Context, string) (domain.SteamDTFilterResult, error) {
	return s.result, nil
}

func TestDMarketCrawlerRejectsBeforeLastSales(t *testing.T) {
	sales := &crawlerSalesStub{}
	store := &crawlerStoreStub{}
	crawler := NewDMarketMarketCrawler(
		testCrawlerConfig(),
		crawlerCatalogStub{},
		sales,
		store,
		crawlerFilterStub{result: domain.SteamDTFilterResult{Reason: "steamdt_snapshots_missing"}},
	)

	if err := crawler.RunOnce(context.Background()); err != nil {
		t.Fatal(err)
	}
	if sales.calls != 0 {
		t.Fatalf("last-sales called %d times for rejected item", sales.calls)
	}
	if store.calls != 1 || len(store.items) != 0 {
		t.Fatalf("store calls=%d items=%d, want one empty replacement", store.calls, len(store.items))
	}
}

func TestDMarketCrawlerPersistsEligibleOpportunity(t *testing.T) {
	sales := &crawlerSalesStub{}
	store := &crawlerStoreStub{}
	crawler := NewDMarketMarketCrawler(
		testCrawlerConfig(),
		crawlerCatalogStub{},
		sales,
		store,
		crawlerFilterStub{result: domain.SteamDTFilterResult{
			Eligible:       true,
			Reason:         "steamdt_liquidity_ok",
			PlatformCount:  5,
			TotalSellCount: 100,
			TotalBidCount:  50,
		}},
	)

	if err := crawler.RunOnce(context.Background()); err != nil {
		t.Fatal(err)
	}
	if sales.calls != 1 || store.calls != 1 || len(store.items) != 1 {
		t.Fatalf("sales=%d store=%d items=%d", sales.calls, store.calls, len(store.items))
	}
	if !store.items[0].SteamDT.Eligible {
		t.Fatal("eligible SteamDT result was not preserved")
	}
}

func testCrawlerConfig() config.DMarketCrawlerConfig {
	return config.DMarketCrawlerConfig{
		Enabled:           true,
		GameID:            "a8db",
		Currency:          "USD",
		Limit:             100,
		MaxPages:          1,
		PriceFromUSD:      1,
		PriceToUSD:        100,
		MinProfitUSD:      1,
		MinROIPercent:     3,
		LastSalesLimit:    10,
		MinLastSalesCount: 1,
	}
}

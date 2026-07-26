package dmarket

import (
	"context"
	"crypto/ed25519"
	"net/http"
	"net/http/httptest"
	"testing"

	"back/internal/ports"
)

func TestListMarketItemsUsesMarketplaceV2Offers(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/marketplace-api/v2/offers" {
			t.Fatalf("path=%q", r.URL.Path)
		}
		query := r.URL.Query()
		if query.Get("gameId") != "a8db" || query.Get("priceFrom") != "500" || query.Get("priceTo") != "50000" {
			t.Fatalf("unexpected query: %s", r.URL.RawQuery)
		}
		if query.Get("limit") != "100" || query.Get("orderBy") != "price" || query.Get("orderDir") != "asc" {
			t.Fatalf("unexpected paging/sort query: %s", r.URL.RawQuery)
		}
		if query.Get("currency") != "" {
			t.Fatalf("retired currency query was sent: %s", r.URL.RawQuery)
		}

		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{
			"items": [
				{"offerId":"one","priceCents":"1599","attributes":{"title":"AK-47 | Redline (Field-Tested)","gameId":"a8db"}},
				{"offerId":"two","priceCents":2500,"attributes":{"title":"M4A1-S | Test"}},
				{"offerId":"missing-title","priceCents":100,"attributes":{}}
			],
			"cursor":"next-page"
		}`))
	}))
	defer server.Close()

	client := &Client{
		baseURL:    server.URL,
		httpClient: server.Client(),
		publicKey:  "test-public",
		secretKey:  ed25519.NewKeyFromSeed(make([]byte, ed25519.SeedSize)),
	}

	items, cursor, err := client.ListMarketItems(context.Background(), ports.MarketItemsRequest{
		GameID:       "a8db",
		Currency:     "USD",
		PriceFromUSD: 5,
		PriceToUSD:   500,
		Limit:        150,
	})
	if err != nil {
		t.Fatal(err)
	}
	if cursor != "next-page" {
		t.Fatalf("cursor=%q", cursor)
	}
	if len(items) != 2 {
		t.Fatalf("items=%#v", items)
	}
	if items[0].BestOfferUSD != 15.99 || items[0].GameID != "a8db" {
		t.Fatalf("first item=%#v", items[0])
	}
	if items[1].BestOfferUSD != 25 || items[1].GameID != "a8db" {
		t.Fatalf("fallback game item=%#v", items[1])
	}
}

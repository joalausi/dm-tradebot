package dmarket

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"

	"back/internal/domain"
	"back/internal/ports"
)

// в /exchange/v1/market/items цены обычн идут стрингами в price.USD
// потому для запросов priceFrom/priceTo лучше исподльзовать чем usd cents
type marketItemsResponse struct {
	Objects []struct {
		Title  string `json:"title"`
		GameID string `json:"gameId"`
		Price  struct {
			USD string `json:"USD"`
		} `json:"price"`
	} `json:"objects"`
	Cursor string `json:"cursor"`
}

func (c *Client) ListMarketItems(
	ctx context.Context,
	req ports.MarketItemsRequest,
) ([]domain.MarketItem, string, error) {
	q := url.Values{}
	q.Set("gameId", req.GameID)
	q.Set("currency", req.Currency)

	if req.Limit <= 0 {
		req.Limit = 100
	}
	q.Set("limit", strconv.Itoa(req.Limit))

	if req.Cursor != "" {
		q.Set("cursor", req.Cursor)
	}

	if req.PriceFromUSD > 0 {
		q.Set("priceFrom", strconv.Itoa(usdToCoins(req.PriceFromUSD)))
	}
	if req.PriceToUSD > 0 {
		q.Set("priceTo", strconv.Itoa(usdToCoins(req.PriceToUSD)))
	}

	// пока для MVP лучш идти от дешёвых к дорогим
	q.Set("orderBy", "price")
	q.Set("orderDir", "asc")

	raw, err := c.doSigned(ctx, "GET", "/exchange/v1/market/items", q, nil)
	if err != nil {
		return nil, "", fmt.Errorf("market items: %w", err)
	}

	var resp marketItemsResponse
	if err := json.Unmarshal(raw, &resp); err != nil {
		return nil, "", fmt.Errorf("decode market items: %w", err)
	}

	out := make([]domain.MarketItem, 0, len(resp.Objects))

	for _, obj := range resp.Objects {
		if obj.Title == "" {
			continue
		}

		price := coinsStringToUSD(obj.Price.USD)

		out = append(out, domain.MarketItem{
			Title:        obj.Title,
			GameID:       obj.GameID,
			BestOfferUSD: price,
		})
	}

	return out, resp.Cursor, nil
}

// ------------------------------- helpers
func usdToCoins(v float64) int {
	return int(v*100 + 0.5)
}

func coinsStringToUSD(s string) float64 {
	v, err := strconv.ParseFloat(s, 64)
	if err != nil {
		return 0
	}

	return v / 100.0
}

//-------------------------------

func (c *Client) AggregatedPrices(
	ctx context.Context,
	gameID string,
	titles []string,
) ([]domain.Opportunity, error) {
	if len(titles) == 0 {
		return nil, nil
	}

	reqBody := map[string]any{
		"cursor": "",
		"limit":  strconv.Itoa(len(titles)),
		"filter": map[string]any{
			"game":   gameID,
			"titles": titles,
		},
	}

	bodyBytes, err := json.Marshal(reqBody)
	if err != nil {
		return nil, fmt.Errorf("marshal aggregated-prices: %w", err)
	}

	raw, err := c.doSigned(ctx, "POST", "/marketplace-api/v1/aggregated-prices", nil, bodyBytes)
	if err != nil {
		return nil, fmt.Errorf("aggregated-prices: %w", err)
	}

	var resp aggregatedPricesResponse
	if err := json.Unmarshal(raw, &resp); err != nil {
		return nil, fmt.Errorf("decode aggregated-prices: %w", err)
	}

	out := make([]domain.Opportunity, 0, len(resp.AggregatedPrices))

	for _, ap := range resp.AggregatedPrices {
		bestTarget, _ := strconv.ParseFloat(ap.OrderBestPrice.Amount, 64)
		bestOffer, _ := strconv.ParseFloat(ap.OfferBestPrice.Amount, 64)

		// aggregated-prices у тебя уже проверено отдаёт cents.
		bestTarget = bestTarget / 100.0
		bestOffer = bestOffer / 100.0

		targetCount, _ := strconv.Atoi(ap.OrderCount)
		offerCount, _ := strconv.Atoi(ap.OfferCount)

		out = append(out, domain.Opportunity{
			GameID: gameID,
			Title:  ap.Title,

			BestTargetUSD: bestTarget,
			TargetCount:   targetCount,

			BestOfferUSD: bestOffer,
			OfferCount:   offerCount,
		})
	}

	return out, nil
}

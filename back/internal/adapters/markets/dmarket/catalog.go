package dmarket

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"

	"back/internal/domain"
	"back/internal/ports"
)

type marketItemsResponse struct {
	Items []struct {
		OfferID    string `json:"offerId"`
		PriceCents int64  `json:"priceCents"`
		Attributes struct {
			Title  string `json:"title"`
			GameID string `json:"gameId"`
		} `json:"attributes"`
	} `json:"items"`
	Cursor string `json:"cursor"`
}

func (c *Client) ListMarketItems(
	ctx context.Context,
	req ports.MarketItemsRequest,
) ([]domain.MarketItem, string, error) {
	q := url.Values{}
	q.Set("gameId", req.GameID)

	if req.Limit <= 0 {
		req.Limit = 100
	}
	if req.Limit > 100 {
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

	raw, err := c.doSigned(ctx, http.MethodGet, "/marketplace-api/v2/offers", q, nil)
	if err != nil {
		return nil, "", fmt.Errorf("market items: %w", err)
	}

	var resp marketItemsResponse
	if err := json.Unmarshal(raw, &resp); err != nil {
		return nil, "", fmt.Errorf("decode market items: %w", err)
	}

	out := make([]domain.MarketItem, 0, len(resp.Items))

	for _, item := range resp.Items {
		if item.Attributes.Title == "" || item.PriceCents <= 0 {
			continue
		}

		gameID := item.Attributes.GameID
		if gameID == "" {
			gameID = req.GameID
		}

		out = append(out, domain.MarketItem{
			Title:        item.Attributes.Title,
			GameID:       gameID,
			BestOfferUSD: float64(item.PriceCents) / 100.0,
		})
	}

	return out, resp.Cursor, nil
}

// ------------------------------- helpers
func usdToCoins(v float64) int {
	return int(v*100 + 0.5)
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

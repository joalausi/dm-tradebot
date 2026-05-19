package dmarket

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"
	"time"

	"back/internal/domain"
)

type lastSalesResponse struct {
	Sales []struct {
		Price           string `json:"price"`
		Date            string `json:"date"`
		TxOperationType string `json:"txOperationType"`
	} `json:"sales"`
}

func (c *Client) LastSales(
	ctx context.Context,
	gameID, title string,
	limit int,
) ([]domain.Sale, error) {
	if limit <= 0 {
		limit = 20
	}

	q := url.Values{}
	q.Set("gameId", gameID)
	q.Set("title", title)
	q.Set("limit", strconv.Itoa(limit))
	q.Set("offset", "0")

	raw, err := c.doSigned(ctx, "GET", "/trade-aggregator/v1/last-sales", q, nil)
	if err != nil {
		return nil, fmt.Errorf("last-sales: %w", err)
	}

	var resp lastSalesResponse
	if err := json.Unmarshal(raw, &resp); err != nil {
		return nil, fmt.Errorf("decode last-sales: %w", err)
	}

	out := make([]domain.Sale, 0, len(resp.Sales))

	for _, s := range resp.Sales {
		price, err := strconv.ParseFloat(s.Price, 64)
		if err != nil || price <= 0 {
			continue
		}

		ts, _ := strconv.ParseInt(s.Date, 10, 64)

		out = append(out, domain.Sale{
			GameID:   gameID,
			Title:    title,
			PriceUSD: price,
			SoldAt:   time.Unix(ts, 0),
		})
	}

	return out, nil
}

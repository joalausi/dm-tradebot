package adapters

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"back/internal/core"
)

const dmarketBaseURL = "https://api.dmarket.com"

type DMarketClient struct {
	httpClient *http.Client
}

func NewDMarketClient() core.MarketData {
	return &DMarketClient{
		httpClient: &http.Client{Timeout: 15 * time.Second},
	}
}

// DepthByTitle возвращает стакан (bids/asks) для конкретного gameID + title.
// Внутри делает два запроса:
//   1) GET /marketplace-api/v1/targets-by-title/{gameId}/{title}   -> bids (targets)
//   2) GET /exchange/v1/offers-by-title?Title=...&Limit=50        -> asks (offers)
func (c *DMarketClient) DepthByTitle(ctx context.Context, gameID, title string, topN int) (core.Depth, error) {
	if topN <= 0 {
		topN = 5
	}
	// в swagger: get /marketplace-api/v1/targets-by-title/{game_id}/{title}
		// важно: title - часть PATH, so  PathEscape.
	targetsURL := fmt.Sprintf("%s/marketplace-api/v1/targets-by-title/%s/%s",
		dmarketBaseURL, gameID, url.PathEscape(title))

		body, err := c.httpGet(ctx, targetsURL)
	if err != nil {
		return core.Depth{}, fmt.Errorf("targets-by-title: %w", err)
	}

	// ключ "orders" (по доке)
	bids, err := parseLevels(body, []string{"orders", "targets", "bids"})
	if err != nil {
		return core.Depth{}, fmt.Errorf("parse targets: %w", err)
	}
	if len(bids) > topN {
		bids = bids[:topN]
	}

	// --------- asks offers / BIDS / TARGETS ---------
	// В swagger: get /exchange/v1/offers-by-title?Title=...&Limit=...
	offersURL, _ := url.Parse(dmarketBaseURL + "/exchange/v1/offers-by-title")
	q := offersURL.Query()
	q.Set("Title", title)     // Query-параметр, НЕ path
	q.Set("Limit", "50")      // с запасом; ниже обрежем до topN
	offersURL.RawQuery = q.Encode()

	body2, err := c.httpGet(ctx, offersURL.String())
	if err != nil {
		return core.Depth{}, fmt.Errorf("offers-by-title: %w", err)
	}

	// В ответе корневой ключ "objects" (по доке),
	// на всякий "offers"/"asks"/"items".
	asks, err := parseLevels(body2, []string{"objects", "offers", "asks", "items"})
	if err != nil {
		return core.Depth{}, fmt.Errorf("parse offers: %w", err)
	}
	if len(asks) > topN {
		asks = asks[:topN]
	}

	return core.Depth{
		Bids: bids,
		Asks: asks,
	}, nil
}
// --------------------------------- низкоуровневые helpers ---------------------------------

func (c *DMarketClient) httpGet(ctx context.Context, urlStr string) ([]byte, error) {
	req, _ := http.NewRequestWithContext(ctx, http.MethodGet, urlStr, nil)
	req.Header.Set("Accept", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	body, _ := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
	if resp.StatusCode >= 300 {
		return nil, fmt.Errorf("status %d: %s", resp.StatusCode, trimBody(body))
	}
	return body, nil
}

func trimBody(b []byte) string {
	s := string(b)
	if len(s) > 300 {
		return s[:300] + "..."
	}
	return s
}

// parseLevels - универсальный парсер уровней цен.
// Он ищет массив по одному из rootKeys (например, "orders" или "objects"),
// а внутри каждого элемента пытается вытащить price и qty из разных форматов.
func parseLevels(body []byte, rootKeys []string) ([]core.PriceLevel, error) {
	var raw any
	if err := json.Unmarshal(body, &raw); err != nil {
		return nil, err
	}
	root, ok := raw.(map[string]any)
	if !ok {
		// неожиданный формат, но это не краш
		return nil, nil
	}
	// иногда данные лежат под "data"
	if data, ok := root["data"].(map[string]any); ok {
		root = data
	}

	var arr []any
	for _, k := range rootKeys {
		if v, ok := root[k]; ok {
			if slice, ok := v.([]any); ok {
				arr = slice
				break
			}
		}
	}
	if arr == nil {
		// для дебага можно раскомментировать:
		// fmt.Println("[debug] no array for keys", rootKeys, "in", string(body))
		return nil, nil
	}

	out := make([]core.PriceLevel, 0, len(arr))
	for _, el := range arr {
		m, ok := el.(map[string]any)
		if !ok {
			continue
		}
		price := parsePriceFromMap(m)
		if price <= 0 {
			continue
		}
		qty := parseQtyFromMap(m)
		if qty <= 0 {
			qty = 1
		}
		out = append(out, core.PriceLevel{
			Price: price,
			Qty:   qty,
		})
	}
	return out, nil
}

// parsePriceFromMap пытается вытащить цену из разных вариантов поля.
func parsePriceFromMap(m map[string]any) float64 {
	// 1) price: float|string|{"USD":"12.34"}|{"Amount":"12.34"}
	if v, ok := m["price"]; ok {
		if f, ok := toFloat(v); ok {
			return f
		}
		if mm, ok := v.(map[string]any); ok {
			if usd, ok := mm["USD"]; ok {
				if f, ok := toFloat(usd); ok {
					return f
				}
			}
			if amt, ok := mm["Amount"]; ok {
				if f, ok := toFloat(amt); ok {
					return f
				}
			}
		}
	}
	// 2) priceUSD: "12.34"
	if v, ok := m["priceUSD"]; ok {
		if f, ok := toFloat(v); ok {
			return f
		}
	}
	return 0
}

// parseQtyFromMap пытается вытащить количество из amount/qty/count/available.
func parseQtyFromMap(m map[string]any) int {
	for _, k := range []string{"amount", "qty", "count", "available"} {
		if v, ok := m[k]; ok {
			if f, ok := toFloat(v); ok {
				if f <= 0 {
					return 0
				}
				return int(f)
			}
		}
	}
	return 0
}

// toFloat конвертирует float64|string в float64.
func toFloat(v any) (float64, bool) {
	switch x := v.(type) {
	case float64:
		return x, true
	case string:
		x = strings.TrimSpace(strings.ReplaceAll(x, ",", ""))
		if x == "" {
			return 0, false
		}
		f, err := strconv.ParseFloat(x, 64)
		if err != nil {
			return 0, false
		}
		return f, true
	default:
		return 0, false
	}
}

	// он должен:
	//  - сделать GET targets-by-title
	//  - сделать GET offers-by-title
	//  - распарсить JSON в []PriceLevel
	//  - вернуть core.Depth{Bids: bids, Asks: asks}
	// в случае ошибки вернуть её.
	//
	// ниже  прост заглушка, чтобы компилилось
package dmarket

import (
	"bytes"
	"context"
	"crypto/ed25519"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"
	"time"

	"back/internal/domain"
)

const dmarketBaseURL = "https://api.dmarket.com"

type Client struct {
	baseURL    string
	httpClient *http.Client
	publicKey  string
	secretKey  ed25519.PrivateKey
}

type userTargetsResponse struct {
	Items  []userTargetDTO `json:"Items"`
	Total  string          `json:"Total"`
	Cursor string          `json:"Cursor"`
}

type userTargetDTO struct {
	TargetID string `json:"TargetID"`
	Title    string `json:"Title"`
	Amount   string `json:"Amount"`
	Status   string `json:"Status"`
	GameID   string `json:"GameID"`
	Price    struct {
		Currency string  `json:"Currency"`
		Amount   float64 `json:"Amount"`
	} `json:"Price"`
	Attributes []struct {
		Name  string `json:"Name"`
		Value string `json:"Value"`
	} `json:"Attributes"`
}

func (c *Client) ListUserTargets(ctx context.Context, gameID string, statuses []string) ([]domain.UserTarget, error) {
	if len(statuses) == 0 {
		statuses = []string{"TargetStatusActive", "TargetStatusInactive"}
	}

	var out []domain.UserTarget
	seen := make(map[string]bool)

	for _, status := range statuses {
		cursor := ""

		for {
			q := url.Values{}
			q.Set("GameID", gameID)
			q.Set("Limit", "100")
			q.Set("BasicFilters.Status", status)

			if cursor != "" {
				q.Set("Cursor", cursor)
			}

			raw, err := c.doSigned(ctx, http.MethodGet, "/marketplace-api/v1/user-targets", q, nil)
			if err != nil {
				return nil, fmt.Errorf("user-targets %s: %w", status, err)
			}

			var resp userTargetsResponse
			if err := json.Unmarshal(raw, &resp); err != nil {
				return nil, fmt.Errorf("decode user-targets: %w", err)
			}

			for _, item := range resp.Items {
				if item.TargetID != "" && seen[item.TargetID] {
					continue
				}
				if item.TargetID != "" {
					seen[item.TargetID] = true
				}

				amount, _ := strconv.Atoi(item.Amount)
				if amount <= 0 {
					amount = 1
				}

				attrs := make([]domain.TargetAttribute, 0, len(item.Attributes))
				for _, a := range item.Attributes {
					attrs = append(attrs, domain.TargetAttribute{
						Name:  a.Name,
						Value: a.Value,
					})
				}

				out = append(out, domain.UserTarget{
					TargetID:   item.TargetID,
					Title:      item.Title,
					GameID:     item.GameID,
					Status:     item.Status,
					PriceUSD:   item.Price.Amount,
					Amount:     amount,
					Attributes: attrs,
				})
			}

			if resp.Cursor == "" {
				break
			}
			cursor = resp.Cursor
		}
	}

	return out, nil
}

func NewClient() (*Client, error) {
	publicKey := strings.TrimSpace(os.Getenv("DMARKET_PUBLIC_KEY"))
	secretHex := strings.TrimSpace(os.Getenv("DMARKET_SECRET_KEY"))

	if publicKey == "" || secretHex == "" {
		return nil, fmt.Errorf("set DMARKET_PUBLIC_KEY and DMARKET_SECRET_KEY env vars")
	}

	secretBytes, err := hex.DecodeString(secretHex)
	if err != nil {
		return nil, fmt.Errorf("decode DMarket secret key: %w", err)
	}
	if len(secretBytes) != ed25519.PrivateKeySize {
		return nil, fmt.Errorf("DMarket secret key length = %d, want %d", len(secretBytes), ed25519.PrivateKeySize)
	}

	return &Client{
		baseURL:    dmarketBaseURL,
		httpClient: &http.Client{Timeout: 15 * time.Second},
		publicKey:  publicKey,
		secretKey:  ed25519.PrivateKey(secretBytes),
	}, nil
}

type aggregatedPricesResponse struct {
	AggregatedPrices []struct {
		Title          string `json:"title"`
		OrderBestPrice struct {
			Currency string `json:"Currency"`
			Amount   string `json:"Amount"`
		} `json:"orderBestPrice"`
		OrderCount     string `json:"orderCount"`
		OfferBestPrice struct {
			Currency string `json:"Currency"`
			Amount   string `json:"Amount"`
		} `json:"offerBestPrice"`
		OfferCount string `json:"offerCount"`
	} `json:"aggregatedPrices"`
	NextCursor string `json:"nextCursor"`
}

// DepthByTitle возвращает стакан (bids/asks) для конкретного gameID + title.
// Внутри делает два запроса:
//  1. GET /marketplace-api/v1/targets-by-title/{gameId}/{title}   -> bids (targets)
//  2. GET /exchange/v1/offers-by-title?Title=...&Limit=50        -> asks (offers)
func (c *Client) DepthByTitle(ctx context.Context, gameID, title string, topN int) (domain.Depth, error) {
	// Собираем body как в swagger:
	// {
	//   "cursor": "",
	//   "limit": "1",
	//   "filter": { "game": "<gameID>", "titles": ["<title>"] }
	// }
	reqBody := map[string]any{
		"cursor": "",
		"limit":  "1",
		"filter": map[string]any{
			"game":   gameID,
			"titles": []string{title},
		},
	}

	bodyBytes, err := json.Marshal(reqBody)
	if err != nil {
		return domain.Depth{}, fmt.Errorf("marshal aggregated-prices body: %w", err)
	}

	raw, err := c.doSigned(ctx, http.MethodPost, "/marketplace-api/v1/aggregated-prices", nil, bodyBytes)
	if err != nil {
		return domain.Depth{}, fmt.Errorf("aggregated-prices: %w", err)
	}

	var resp aggregatedPricesResponse
	if err := json.Unmarshal(raw, &resp); err != nil {
		return domain.Depth{}, fmt.Errorf("decode aggregated-prices: %w", err)
	}

	if len(resp.AggregatedPrices) == 0 {
		// Нет ордеров/офферов по этому title — просто пустой стакан.
		return domain.Depth{}, nil
	}

	ap := resp.AggregatedPrices[0]

	var depth domain.Depth

	// aggregated-prices возвращает цены в cents for USD, поэтому делим на 100
	// best target (ордеры / buy side)
	if ap.OrderBestPrice.Amount != "" {
		if price, err := strconv.ParseFloat(ap.OrderBestPrice.Amount, 64); err == nil {
			price = price / 100.0

			qty, _ := strconv.Atoi(ap.OrderCount)
			depth.Bids = append(depth.Bids, domain.PriceLevel{
				Price: price,
				Qty:   qty,
			})
		}
	}

	// best offer (офферы / sell side)
	if ap.OfferBestPrice.Amount != "" {
		if price, err := strconv.ParseFloat(ap.OfferBestPrice.Amount, 64); err == nil {
			price = price / 100.0

			qty, _ := strconv.Atoi(ap.OfferCount)
			depth.Asks = append(depth.Asks, domain.PriceLevel{
				Price: price,
				Qty:   qty,
			})
		}
	}

	// topN пока игнорируем, т.к. агрегатор даёт только best price;
	// интерфейс оставляем на будущее.
	return depth, nil
}

// --------------------------------- низкоуровневые helpers ---------------------------------

func (c *Client) httpGet(ctx context.Context, urlStr string) ([]byte, error) {
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

// doSigned выполняет запрос с подписью Trading API и возвращает тело ответа.
func (c *Client) doSigned(
	ctx context.Context,
	method, path string,
	query url.Values,
	body []byte,
) ([]byte, error) {
	uri := path
	if query != nil {
		qs := query.Encode()
		if qs != "" {
			uri = path + "?" + qs
		}
	}

	ts := strconv.FormatInt(time.Now().Unix(), 10)
	stringToSign := method + uri + string(body) + ts

	sig := ed25519.Sign(c.secretKey, []byte(stringToSign))
	sigHex := hex.EncodeToString(sig)

	req, err := http.NewRequestWithContext(ctx, method, c.baseURL+uri, bytes.NewReader(body))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Accept", "application/json")
	if len(body) > 0 {
		req.Header.Set("Content-Type", "application/json")
	}
	req.Header.Set("X-Api-Key", c.publicKey)
	req.Header.Set("X-Sign-Date", ts)
	req.Header.Set("X-Request-Sign", "dmar ed25519 "+sigHex)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
	if err != nil {
		return nil, err
	}
	if resp.StatusCode >= 300 {
		return nil, fmt.Errorf("%s %s: status %d: %s", method, path, resp.StatusCode, trimBody(respBody))
	}
	return respBody, nil
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
func parseLevels(body []byte, rootKeys []string) ([]domain.PriceLevel, error) {
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

	out := make([]domain.PriceLevel, 0, len(arr))
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
		out = append(out, domain.PriceLevel{
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
	// offers-by-title: price: {"USD": "123.45", ...}
	if priceObj, ok := m["price"].(map[string]any); ok {
		if usd, ok := priceObj["USD"]; ok {
			if f, ok := toFloat(usd); ok {
				return f
			}
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

func (c *Client) PingUserTargets(ctx context.Context) error {
	path := "/marketplace-api/v1/user-targets"
	q := url.Values{}
	q.Set("GameID", "a8db") // CS2

	body, err := c.doSigned(ctx, http.MethodGet, path, q, nil)
	if err != nil {
		return fmt.Errorf("user-targets: %w", err)
	}

	fmt.Printf("[ping] user-targets OK: %s\n", trimBody(body))
	return nil
}

// он должен:
//  - сделать GET targets-by-title
//  - сделать GET offers-by-title
//  - распарсить JSON в []PriceLevel
//  - вернуть domain.Depth{Bids: bids, Asks: asks}
// в случае ошибки вернуть её.
//
// ниже  прост заглушка, чтобы компилилось

package adapters

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

	"back/internal/core"
)

const dmarketBaseURL = "https://api.dmarket.com"

type DMarketClient struct {
    baseURL    string
    httpClient *http.Client
    publicKey  string
    secretKey  ed25519.PrivateKey
}

func NewDMarketClient() (*DMarketClient, error) {
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

    return &DMarketClient{
        baseURL:    dmarketBaseURL,
        httpClient: &http.Client{Timeout: 15 * time.Second},
        publicKey:  publicKey,
        secretKey:  ed25519.PrivateKey(secretBytes),
    }, nil
}

// DepthByTitle возвращает стакан (bids/asks) для конкретного gameID + title.
// Внутри делает два запроса:
//   1) GET /marketplace-api/v1/targets-by-title/{gameId}/{title}   -> bids (targets)
//   2) GET /exchange/v1/offers-by-title?Title=...&Limit=50        -> asks (offers)
func (c *DMarketClient) DepthByTitle(ctx context.Context, gameID, title string, topN int) (core.Depth, error) {
	if topN <= 0 {
		topN = 5
	}

// --------- BIDS / TARGETS ------
	// в swagger: get /marketplace-api/v1/targets-by-title/{game_id}/{title}
		// важно: title - часть PATH, so  PathEscape.
	pathTargets := fmt.Sprintf("/marketplace-api/v1/targets-by-title/%s/%s",
        gameID, url.PathEscape(title))

		bodyTargets, err := c.doSigned(ctx, http.MethodGet, pathTargets, nil, nil)
    if err != nil {
        return core.Depth{}, fmt.Errorf("targets-by-title: %w", err)
    }

	// ключ "orders" (по доке)
	bids, err := parseLevels(bodyTargets, []string{"orders"})
	if err != nil {
		return core.Depth{}, fmt.Errorf("parse targets: %w", err)
	}
	if len(bids) > topN {
		bids = bids[:topN]
	}

	// --------- asks offers / BIDS / TARGETS ---------
	// В swagger: get /exchange/v1/offers-by-title?Title=...&Limit=...
	pathOffers := "/exchange/v1/offers-by-title"
    q := url.Values{}
    q.Set("Title", title)
    q.Set("Limit", "50")

    bodyOffers, err := c.doSigned(ctx, http.MethodGet, pathOffers, q, nil)
	if err != nil {
		return core.Depth{}, fmt.Errorf("offers-by-title: %w", err)
	}

	// В ответе корневой ключ "objects" (по доке),
	// на всякий "offers"/"asks"/"items".
	asks, err := parseLevels(bodyOffers, []string{"objects"})
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

// doSigned выполняет запрос с подписью Trading API и возвращает тело ответа.
func (c *DMarketClient) doSigned(
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

func (c *DMarketClient) PingUserTargets(ctx context.Context) error {
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
	//  - вернуть core.Depth{Bids: bids, Asks: asks}
	// в случае ошибки вернуть её.
	//
	// ниже  прост заглушка, чтобы компилилось
package steamdt

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"
)

const defaultBaseURL = "https://open.steamdt.com"

type Client struct {
	baseURL    string
	apiKey     string
	httpClient *http.Client
}

type PriceSingleResponse struct {
	Success      bool            `json:"success"`
	Data         []PlatformPrice `json:"data"`
	ErrorCode    int             `json:"errorCode"`
	ErrorMsg     string          `json:"errorMsg"`
	ErrorData    json.RawMessage `json:"errorData"`
	ErrorCodeStr string          `json:"errorCodeStr"`
}

type PlatformPrice struct {
	Platform       string  `json:"platform"`
	PlatformItemID string  `json:"platformItemId"`
	SellPrice      float64 `json:"sellPrice"`
	SellCount      int     `json:"sellCount"`
	BiddingPrice   float64 `json:"biddingPrice"`
	BiddingCount   int     `json:"biddingCount"`
	UpdateTime     int64   `json:"updateTime"`
}

type PriceBatchResponse struct {
	Success      bool             `json:"success"`
	Data         []BatchItemPrice `json:"data"`
	ErrorCode    int              `json:"errorCode"`
	ErrorMsg     string           `json:"errorMsg"`
	ErrorData    json.RawMessage  `json:"errorData"`
	ErrorCodeStr string           `json:"errorCodeStr"`
}

type BatchItemPrice struct {
	MarketHashName string          `json:"marketHashName"`
	DataList       []PlatformPrice `json:"dataList"`
}

func NewClient() (*Client, error) {
	apiKey := strings.TrimSpace(os.Getenv("STEAMDT_API_KEY"))
	if apiKey == "" {
		return nil, fmt.Errorf("STEAMDT_API_KEY is empty")
	}

	return &Client{
		baseURL: defaultBaseURL,
		apiKey:  apiKey,
		httpClient: &http.Client{
			Timeout: 15 * time.Second,
		},
	}, nil
}

func (c *Client) FetchPriceSingle(ctx context.Context, marketHashName string) (PriceSingleResponse, error) {
	var out PriceSingleResponse

	marketHashName = strings.TrimSpace(marketHashName)
	if marketHashName == "" {
		return out, fmt.Errorf("market hash name is empty")
	}

	endpoint := c.baseURL + "/open/cs2/v1/price/single?marketHashName=" + url.QueryEscape(marketHashName)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return out, err
	}

	req.Header.Set("Authorization", "Bearer "+c.apiKey)
	req.Header.Set("Accept", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return out, err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
	if err != nil {
		return out, err
	}

	if resp.StatusCode >= 300 {
		return out, fmt.Errorf("steamdt status %d: %s", resp.StatusCode, trimBody(body))
	}

	if err := json.Unmarshal(body, &out); err != nil {
		return out, fmt.Errorf("decode steamdt response: %w", err)
	}

	if !out.Success {
		return out, fmt.Errorf("steamdt error %d: %s", out.ErrorCode, out.ErrorMsg)
	}

	return out, nil
}

func (c *Client) FetchPriceBatch(ctx context.Context, marketHashNames []string) (PriceBatchResponse, error) {
	var out PriceBatchResponse

	cleaned := make([]string, 0, len(marketHashNames))
	seen := make(map[string]struct{}, len(marketHashNames))

	for _, name := range marketHashNames {
		name = strings.TrimSpace(name)
		if name == "" {
			continue
		}
		if _, ok := seen[name]; ok {
			continue
		}
		seen[name] = struct{}{}
		cleaned = append(cleaned, name)
	}

	if len(cleaned) == 0 {
		return out, fmt.Errorf("market hash names are empty")
	}

	payload := map[string]any{
		"marketHashNames": cleaned,
	}

	bodyBytes, err := json.Marshal(payload)
	if err != nil {
		return out, fmt.Errorf("marshal steamdt batch body: %w", err)
	}

	endpoint := c.baseURL + "/open/cs2/v1/price/batch"

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(bodyBytes))
	if err != nil {
		return out, err
	}

	req.Header.Set("Authorization", "Bearer "+c.apiKey)
	req.Header.Set("Accept", "application/json")
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return out, err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(io.LimitReader(resp.Body, 2<<20))
	if err != nil {
		return out, err
	}

	if resp.StatusCode >= 300 {
		return out, fmt.Errorf("steamdt batch status %d: %s", resp.StatusCode, trimBody(body))
	}

	if err := json.Unmarshal(body, &out); err != nil {
		return out, fmt.Errorf("decode steamdt batch response: %w", err)
	}

	if !out.Success {
		return out, fmt.Errorf("steamdt batch error %d: %s", out.ErrorCode, out.ErrorMsg)
	}

	return out, nil
}

func (c *Client) FetchMany(ctx context.Context, marketHashNames []string) (map[string]PriceSingleResponse, error) {
	batchResp, err := c.FetchPriceBatch(ctx, marketHashNames)
	if err != nil {
		return nil, err
	}

	out := make(map[string]PriceSingleResponse, len(batchResp.Data))

	for _, item := range batchResp.Data {
		out[item.MarketHashName] = PriceSingleResponse{
			Success:      true,
			Data:         item.DataList,
			ErrorCode:    0,
			ErrorMsg:     "",
			ErrorData:    nil,
			ErrorCodeStr: "",
		}
	}

	// чтбы не потерять имена по которым API мог вернуть пустой data
	for _, name := range marketHashNames {
		if _, ok := out[name]; !ok {
			out[name] = PriceSingleResponse{
				Success: true,
				Data:    nil,
			}
		}
	}

	return out, nil
}

func trimBody(b []byte) string {
	s := string(b)
	if len(s) > 500 {
		return s[:500] + "..."
	}
	return s
}

func (c *Client) FetchManySingles(ctx context.Context, marketHashNames []string) (map[string]PriceSingleResponse, error) {
	out := make(map[string]PriceSingleResponse, len(marketHashNames))

	for _, name := range marketHashNames {
		resp, err := c.FetchPriceSingle(ctx, name)
		if err != nil {
			return nil, fmt.Errorf("fetch %q: %w", name, err)
		}
		out[name] = resp
	}

	return out, nil
}

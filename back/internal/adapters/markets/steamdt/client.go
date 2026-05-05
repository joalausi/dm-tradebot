package steamdt

import (
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
	Platform      string  `json:"platform"`
	PlatformItemID string `json:"platformItemId"`
	SellPrice     float64 `json:"sellPrice"`
	SellCount     int     `json:"sellCount"`
	BiddingPrice  float64 `json:"biddingPrice"`
	BiddingCount  int     `json:"biddingCount"`
	UpdateTime    int64   `json:"updateTime"`
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

func trimBody(b []byte) string {
	s := string(b)
	if len(s) > 500 {
		return s[:500] + "..."
	}
	return s
}
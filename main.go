package main

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
	"flag"

	"gopkg.in/yaml.v3"
)

type Config struct {
	PollEvery string         `yaml:"poll_every"`
	Discord   *DiscordCfg    `yaml:"discord"`
	Items     []WatchItemCfg `yaml:"items"`
}

type DiscordCfg struct {
	WebhookURL string `yaml:"webhook_url"`
	Mention    string `yaml:"mention"`
}

// type TelegramCfg struct {
// 	BotToken string `yaml:"bot_token"`
// 	ChatID   string `yaml:"chat_id"`
// }

// type Selector struct {
//   Title string
//   GameID string
//   StatTrak []bool
//   Exteriors []string // ["FN","MW","FT","WW","BS"]
//   FloatMin, FloatMax *float64
// }
// expand -> []Variant {TitleForSide, isST, exterior}

type WatchItemCfg struct {
	Title           string  `yaml:"title"`
	GameID          string  `yaml:"game_id"`
	// StatTrak       []bool	`yaml:"stat_trak"`
	// Exteriors      []string `yaml:"exteriors"`
	// FloatMin      *float64  `yaml:"float_min"`
	// FloatMax      *float64  `yaml:"float_max"`
	MyTargetUSD     float64 `yaml:"my_target_usd"`
	LowAskAlertUSD  float64 `yaml:"low_ask_alert_usd"`
	TopN            int     `yaml:"top_n"`
}

type priceLevel struct {
	Price float64
	Qty   int
}

// кэш последнего статуса, чтобы не слать дубли
type lastState struct {
	Undercut bool
	BestBid  float64
	BestAsk  float64
}

var state = map[string]lastState{}

// endpoint
const (
	hostDM = "https://api.dmarket.com"
	// const marketDepthBase = "https://api.dmarket.com/marketplace-api/v1/market-depth"


	// swagger:
	// GET /marketplace-api/v1/targets-by-title/{gameId}/{title}
	pathTargetsByTitle = "/marketplace-api/v1/targets-by-title"

	// swagger:
	// GET /exchange/v1/offers-by-title?Title=...&Limit=...&Currency=USD[&gameId=...]
	pathOffersByTitle = "/exchange/v1/offers-by-title"
)

func main() {
	checkOnly := flag.Bool("check", false, "check DMarket connectivity and exit")
	runOnceOnly := flag.Bool("once", false, "run a single polling iteration and exit")
	flag.Parse()

	if flag.NArg() < 1 {
		fmt.Println("usage: main [-once|-check] ./config.yaml")
		flag.PrintDefaults()
		os.Exit(1)
	}
	cfgBytes, err := os.ReadFile(flag.Arg(0))
	must(err)
	var cfg Config
	must(yaml.Unmarshal(cfgBytes, &cfg))
	
	if *checkOnly {
		if err := checkConnection(&cfg); err != nil {
			fmt.Fprintln(os.Stderr, "connection check failed ❌:", err)
			os.Exit(1)
		}
		fmt.Println("Connection to DMarket API looks good ✅")
		return
	}

	pollEvery := mustParseDurationDefault(cfg.PollEvery, 10*time.Minute)

	ticker := time.NewTicker(pollEvery)
	defer ticker.Stop()

	// первый прогон сразу
	runOnce(&cfg)

	if *runOnceOnly {
		return
	}

	for range ticker.C {
		runOnce(&cfg)
	}
}

func runOnce(cfg *Config) {
	now := time.Now().Format("2006-01-02 15:04:05")
	fmt.Println()
	fmt.Println("========== DMarket Watch @", now, "==========")
	for _, it := range cfg.Items {
		ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
		depth, raw, err := fetchMarketDepth(ctx, it.GameID, it.Title)
		cancel()
		if err != nil {
			fmt.Printf("[ERR] %s — %v\n", it.Title, err)
			continue
		}
		_ = raw // на всякий
		if it.TopN <= 0 {
			it.TopN = 5
		}

		bestBid := 0.0
		if len(depth.Bids) > 0 {
			bestBid = depth.Bids[0].Price
		}
		bestAsk := 0.0
		if len(depth.Asks) > 0 {
			bestAsk = depth.Asks[0].Price
		}

		// консольная табличка
		fmt.Printf("\n%s (gameId=%s)\n", it.Title, it.GameID)
		fmt.Println("-------------------------------------------------------------")
		fmt.Printf("%-10s | %-20s | %-20s\n", "SIDE", "PRICE USD", "QTY")
		fmt.Println("-------------------------------------------------------------")
		for i, pl := range depth.Bids {
			if i >= it.TopN {
				break
			}
			fmt.Printf("%-10s | %-20.2f | %-20d\n", "TARGET", pl.Price, pl.Qty)
		}
		if len(depth.Bids) == 0 {
			fmt.Printf("%-10s | %-20s | %-20s\n", "TARGET", "-", "-")
		}
		fmt.Println("-------------------------------------------------------------")
		for i, pl := range depth.Asks {
			if i >= it.TopN {
				break
			}
			fmt.Printf("%-10s | %-20.2f | %-20d\n", "OFFER", pl.Price, pl.Qty)
		}
		if len(depth.Asks) == 0 {
			fmt.Printf("%-10s | %-20s | %-20s\n", "OFFER", "-", "-")
		}
		fmt.Println("-------------------------------------------------------------")
		status := "OK"
		undercut := false
		if bestBid > 0 && it.MyTargetUSD > 0 && bestBid > it.MyTargetUSD+1e-9 {
			status = fmt.Sprintf("UNDERCUT by %.2f", bestBid-it.MyTargetUSD)
			undercut = true
		}
		lowAskNote := ""
		if it.LowAskAlertUSD > 0 && bestAsk > 0 && bestAsk <= it.LowAskAlertUSD {
			lowAskNote = fmt.Sprintf(" | LOW ASK ALERT (%.2f <= %.2f)", bestAsk, it.LowAskAlertUSD)
		}
		fmt.Printf("MyTarget: %.2f | BestTarget: %.2f | BestOffer: %.2f | %s%s\n",
			it.MyTargetUSD, bestBid, bestAsk, status, lowAskNote)

		// уведомления — только при изменении состояния
		key := it.GameID + "|" + it.Title
		prev := state[key]
		needPing := false
		var msg strings.Builder
		if undercut && !prev.Undercut {
			needPing = true
			msg.WriteString(fmt.Sprintf("⚠️ Твой таргет перебит\n%s\nBestTarget: %.2f (твой %.2f)", it.Title, bestBid, it.MyTargetUSD))
		}
		if it.LowAskAlertUSD > 0 && bestAsk > 0 && bestAsk <= it.LowAskAlertUSD && (prev.BestAsk == 0 || bestAsk < prev.BestAsk-1e-9) {
			if needPing {
				msg.WriteString("\n")
			}
			needPing = true
			msg.WriteString(fmt.Sprintf("💡 Низкий оффер: %.2f (порог %.2f)", bestAsk, it.LowAskAlertUSD))
		}
		state[key] = lastState{Undercut: undercut, BestBid: bestBid, BestAsk: bestAsk}

		// if needPing && cfg.Telegram != nil && cfg.Telegram.BotToken != "" && cfg.Telegram.ChatID != "" {
		// 	if err := sendTelegram(cfg.Telegram, msg.String()); err != nil {
		// 		fmt.Println("[telegram error]:", err)
		// 	}
		// }
		if needPing && cfg.Discord != nil {
			if cfg.Discord.WebhookURL == "" {
				fmt.Println("[discord warning]: webhook URL is not configured")
			} else if err := sendDiscord(cfg.Discord, msg.String()); err != nil {
				fmt.Println("[discord error]:", err)
			}
		}
	}
		// На всякий закомментируй строку ниже, если сырой JSON не нужен
		// _ = raw
		// fmt.Println(string(raw))
}

// fetchMarketDepth: вытягивает уровни цен. Пытаемся быть толерантными к схеме.
type depthSnapshot struct {
	Bids []priceLevel // targets / buy side (высшие цены сверху)
	Asks []priceLevel // offers / sell side (низшие цены сверху)
}

func fetchMarketDepth(ctx context.Context, gameID, title string) (*depthSnapshot, []byte, error) {
	u, _ := url.Parse(marketDepthBase)
	q := u.Query()
	q.Set("gameId", gameID)
	q.Set("title", title)
	q.Set("currency", "USD")
	q.Set("side", "both")
	// некоторые инсталляции поддерживают limit; если нет — сервер проигнорирует
	q.Set("limit", "50")
	u.RawQuery = q.Encode()

	req, _ := http.NewRequestWithContext(ctx, "GET", u.String(), nil)
	req.Header.Set("Accept", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, nil, err
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
	if resp.StatusCode != 200 {
		return nil, body, fmt.Errorf("status %d: %s", resp.StatusCode, string(body))
	}

	// минимальный «универсальный» парсер: ищем массивы с ценовыми уровнями и поля price/amount
	// ожидается, что ответ содержит что-то вроде:
	// { ..., "targets":[{"price":12.34,"amount":5},...], "offers":[{"price":15.67,"amount":2},...] }
	// или вложенный "data": {...}
	var v any
	if err := json.Unmarshal(body, &v); err != nil {
		return nil, body, err
	}

	// helper: извлечь уровни по возможным ключам
	extractSide := func(m map[string]any, keys ...string) []priceLevel {
		for _, k := range keys {
			if raw, ok := m[k]; ok {
				if arr, ok := raw.([]any); ok {
					out := make([]priceLevel, 0, len(arr))
					for _, el := range arr {
						if mm, ok := el.(map[string]any); ok {
							pl := priceLevel{}
							// цена может быть числом или объектом вида {"USD":"12.34"}
							switch p := mm["price"].(type) {
							case float64:
								pl.Price = p
							case map[string]any:
								if usd, ok := p["USD"].(string); ok {
									if f, err := parseFloat(usd); err == nil {
										pl.Price = f
									}
								}
							case string:
								if f, err := parseFloat(p); err == nil {
									pl.Price = f
								}
							}
							// qty/amount/count
							if a, ok := mm["amount"].(float64); ok {
								pl.Qty = int(a)
							} else if a, ok := mm["count"].(float64); ok {
								pl.Qty = int(a)
							} else if a, ok := mm["qty"].(float64); ok {
								pl.Qty = int(a)
							}
							if pl.Price > 0 {
								out = append(out, pl)
							}
						}
					}
					// для bids хотим сорт по убыванию, для asks — по возрастанию
					// сервер обычно уже сортирует, поэтому тут не пересортировываем
					return out
				}
			}
		}
		return nil
	}

	// распаковываем возможные формы ответа
	var root map[string]any
	switch vv := v.(type) {
	case map[string]any:
		root = vv
	default:
		return &depthSnapshot{}, body, nil
	}
	// если есть "data" – проваливаемся
	if data, ok := root["data"].(map[string]any); ok {
		root = data
	}

	bids := extractSide(root, "targets", "bids", "buy")
	asks := extractSide(root, "offers", "asks", "sell")

	return &depthSnapshot{Bids: bids, Asks: asks}, body, nil
}

func parseFloat(s string) (float64, error) {
	// убрать возможные запятые как разделители
	s = strings.TrimSpace(strings.ReplaceAll(s, ",", ""))
	var f float64
	_, err := fmt.Sscan(s, &f)
	return f, err
}

// func sendTelegram(tg *TelegramCfg, text string) error {
// 	api := "https://api.telegram.org/bot" + tg.BotToken + "/sendMessage"
// 	payload := map[string]any{
// 		"chat_id":    tg.ChatID,
// 		"text":       text,
// 		"parse_mode": "HTML",
// 	}
// 	b, _ := json.Marshal(payload)
// 	resp, err := http.Post(api, "application/json", bytes.NewReader(b))
// 	if err != nil {
// 		return err
// 	}
// 	defer resp.Body.Close()
// 	if resp.StatusCode >= 300 {
// 		body, _ := io.ReadAll(resp.Body)
// 		return fmt.Errorf("telegram %d: %s", resp.StatusCode, string(body))
// 	}
// 	return nil
// }

func must(err error) {
	if err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func mustParseDurationDefault(s string, d time.Duration) time.Duration {
	if s == "" {
		return d
	}
	out, err := time.ParseDuration(s)
	if err != nil {
		return d
	}
	return out
}

func sendDiscord(dc *DiscordCfg, text string) error {
    	// приклеим mention, если указан
	if dc.Mention != "" {
		text = dc.Mention + " " + text
	}

	// Вариант А: webhook
	if dc.WebhookURL != "" {
		payload := map[string]any{
			"content": text,
			// На всякий случай запрещаем массовые @everyone/@here:
			"allowed_mentions": map[string]any{
				"parse": []string{}, // пусто = не парсить everyone/here
			},
		}
		b, _ := json.Marshal(payload)
		resp, err := http.Post(dc.WebhookURL, "application/json", bytes.NewReader(b))
		if err != nil {
			return err
		}
		defer resp.Body.Close()
		if resp.StatusCode >= 300 {
			body, _ := io.ReadAll(resp.Body)
			return fmt.Errorf("discord webhook %d: %s", resp.StatusCode, string(body))
		}
		return nil
	}
	return fmt.Errorf("discord webhook URL is not configured")
}

func checkConnection(cfg *Config) error {
	if len(cfg.Items) == 0 {
		return fmt.Errorf("config has no items to query")
	}
	var errs []string
	for _, it := range cfg.Items {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		_, _, err := fetchMarketDepth(ctx, it.GameID, it.Title)
		cancel()
		if err == nil {
			return nil
		}
		errs = append(errs, fmt.Sprintf("%s: %v", it.Title, err))
	}

	return fmt.Errorf("all probes failed:\n%s", strings.Join(errs, "\n"))
}

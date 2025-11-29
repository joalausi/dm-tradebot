package core

import (
	"context"
	"fmt"
	"time"
	"strings"
)

type Runner struct {
	cfg      Config
	market   MarketData
	notifier Notifier
	state    map[string]lastState
}

type lastState struct {
	Undercut bool
	BestBid  float64
	BestAsk  float64
}

func NewRunner(cfg Config, market MarketData, notifier Notifier) *Runner {
	if notifier == nil {
		notifier = ConsoleNotifier{} // дефолт — просто stdout
	}
	return &Runner{
		cfg:      cfg,
		market:   market,
		notifier: notifier,
		state:    make(map[string]lastState),
	}
}

// Run — бесконечный цикл с тикером.
func (r *Runner) Run(ctx context.Context) error {
	pollEvery, err := time.ParseDuration(r.cfg.PollEvery)
	if err != nil {
		pollEvery = 10 * time.Minute
	}
	ticker := time.NewTicker(pollEvery)
	defer ticker.Stop()

	// первый прогон сразу
	if err := r.RunOnce(ctx); err != nil {
		return err
	}

	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-ticker.C:
			if err := r.RunOnce(ctx); err != nil {
				// логируем и продолжаем
				fmt.Println("[run error]:", err)
			}
		}
	}
}

// RunOnce — одна итерация (удобно для тестов и флага -once)
func (r *Runner) RunOnce(ctx context.Context) error {
	now := time.Now().Format("2006-01-02 15:04:05")
	fmt.Println()
	fmt.Println("========== DMarket Watch @", now, "==========")

	for _, it := range r.cfg.Items {
		depth, err := r.market.DepthByTitle(ctx, it.GameID, it.Title, it.TopN)
		if err != nil {
			fmt.Printf("[ERR] %s — %v\n", it.Title, err)
			continue
		}
		r.printItem(depth, it)
	}
	return nil
}

// Check — примитивный health-check: пытаюсь дернуть первый айтем.
func (r *Runner) Check(ctx context.Context) error {
	if len(r.cfg.Items) == 0 {
		return nil
	}
	it := r.cfg.Items[0]
	_, err := r.market.DepthByTitle(ctx, it.GameID, it.Title, 1)
	return err
}

func (r *Runner) printItem(depth Depth, it Item) {
	bestBid := 0.0
	if len(depth.Bids) > 0 {
		bestBid = depth.Bids[0].Price
	}
	bestAsk := 0.0
	if len(depth.Asks) > 0 {
		bestAsk = depth.Asks[0].Price
	}

	fmt.Printf("\n%s (gameId=%s)\n", it.Title, it.GameID)
	fmt.Println("-------------------------------------------------------------")
	fmt.Printf("%-10s | %-20s | %-20s\n", "SIDE", "PRICE USD", "QTY")
	fmt.Println("-------------------------------------------------------------")
	// bids
	if len(depth.Bids) == 0 {
		fmt.Printf("%-10s | %-20s | %-20s\n", "TARGET", "-", "-")
	} else {
		for i, pl := range depth.Bids {
			if i >= it.TopN {
				break
			}
			fmt.Printf("%-10s | %-20.2f | %-20d\n", "TARGET", pl.Price, pl.Qty)
		}
	}
	fmt.Println("-------------------------------------------------------------")
	// asks
	if len(depth.Asks) == 0 {
		fmt.Printf("%-10s | %-20s | %-20s\n", "OFFER", "-", "-")
	} else {
		for i, pl := range depth.Asks {
			if i >= it.TopN {
				break
			}
			fmt.Printf("%-10s | %-20.2f | %-20d\n", "OFFER", pl.Price, pl.Qty)
		}
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

	key := it.GameID + "|" + it.Title
	prev := r.state[key]

	var messages []string
	if undercut && !prev.Undercut {
		messages = append(messages,
			fmt.Sprintf("⚠️ Твой таргет перебит\n%s\nBestTarget: %.2f (твой %.2f)", it.Title, bestBid, it.MyTargetUSD))
	}
	if it.LowAskAlertUSD > 0 && bestAsk > 0 && bestAsk <= it.LowAskAlertUSD &&
		(prev.BestAsk == 0 || bestAsk < prev.BestAsk-1e-9) {
		messages = append(messages,
			fmt.Sprintf("💡 Низкий оффер: %.2f (порог %.2f)", bestAsk, it.LowAskAlertUSD))
	}

	r.state[key] = lastState{Undercut: undercut, BestBid: bestBid, BestAsk: bestAsk}

	if len(messages) > 0 {
		msg := strings.Join(messages, "\n")
		_ = r.notifier.Notify(context.Background(), msg)
	}
}

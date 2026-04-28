package services

import (
	"context"
	"fmt"
	"strings"
	"time"

	"back/internal/config"
	"back/internal/ports"
	"back/internal/domain"
)

type DMarketTargetRunner struct {
	cfg          config.Config
	market       ports.MarketData
	targetSource ports.UserTargets
	notifier     ports.Notifier
	state        map[string]lastState
}

type lastState struct {
	Undercut bool
	BestBid  float64
	BestAsk  float64
}

func NewDMarketTargetRunner(
	cfg config.Config,
	market ports.MarketData,
	notifier ports.Notifier,
	targetSource ports.UserTargets,
) *DMarketTargetRunner {
	return &DMarketTargetRunner{
		cfg:          cfg,
		market:       market,
		notifier:     notifier,
		targetSource: targetSource,
		state:        make(map[string]lastState),
	}
}

// Run запускает бесконечный цикл мониторинга.
func (r *DMarketTargetRunner) Run(ctx context.Context) error {
	pollEvery, err := time.ParseDuration(r.cfg.PollEvery)
	if err != nil || pollEvery <= 0 {
		pollEvery = 10 * time.Minute
	}

	// Первый прогон сразу после старта.
	if err := r.RunOnce(ctx); err != nil {
		return err
	}

	ticker := time.NewTicker(pollEvery)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return ctx.Err()

		case <-ticker.C:
			if err := r.RunOnce(ctx); err != nil {
				// Не валим весь бот из-за одной итерации.
				fmt.Println("[runner error]:", err)
			}
		}
	}
}

// RunOnce делает одну итерацию мониторинга.
// Это удобно для -once и для тестов.
func (r *DMarketTargetRunner) RunOnce(ctx context.Context) error {
	if r.market == nil {
		return fmt.Errorf("market data adapter is nil")
	}

	now := time.Now().Format("2006-01-02 15:04:05")
	fmt.Println()
	fmt.Println("========== DMarket Watch @", now, "==========")

	items, err := r.resolveItems(ctx)
	if err != nil {
		return fmt.Errorf("resolve items: %w", err)
	}

	for _, it := range items {
		topN := it.TopN
		if topN <= 0 {
			topN = 5
		}

		depth, err := r.market.DepthByTitle(ctx, it.GameID, it.Title, topN)
		if err != nil {
			fmt.Printf("[ERR] %s — %v\n", it.Title, err)
			continue
		}

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

		if len(depth.Bids) == 0 {
			fmt.Printf("%-10s | %-20s | %-20s\n", "TARGET", "-", "-")
		} else {
			for i, pl := range depth.Bids {
				if i >= topN {
					break
				}
				fmt.Printf("%-10s | %-20.2f | %-20d\n", "TARGET", pl.Price, pl.Qty)
			}
		}

		fmt.Println("-------------------------------------------------------------")

		if len(depth.Asks) == 0 {
			fmt.Printf("%-10s | %-20s | %-20s\n", "OFFER", "-", "-")
		} else {
			for i, pl := range depth.Asks {
				if i >= topN {
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

		fmt.Printf(
			"MyTarget: %.2f | BestTarget: %.2f | BestOffer: %.2f | %s%s\n",
			it.MyTargetUSD,
			bestBid,
			bestAsk,
			status,
			lowAskNote,
		)

		key := it.GameID + "|" + it.Title
		prev := r.state[key]

		var messages []string

		if undercut && !prev.Undercut {
			messages = append(messages,
				fmt.Sprintf(
					"⚠️ Твой таргет перебит\n%s\nBestTarget: %.2f (твой %.2f)",
					it.Title,
					bestBid,
					it.MyTargetUSD,
				),
			)
		}

		if it.LowAskAlertUSD > 0 &&
			bestAsk > 0 &&
			bestAsk <= it.LowAskAlertUSD &&
			(prev.BestAsk == 0 || bestAsk < prev.BestAsk-1e-9) {
			messages = append(messages,
				fmt.Sprintf(
					"💡 Низкий оффер\n%s\nBestOffer: %.2f (порог %.2f)",
					it.Title,
					bestAsk,
					it.LowAskAlertUSD,
				),
			)
		}

		r.state[key] = lastState{
			Undercut: undercut,
			BestBid:  bestBid,
			BestAsk:  bestAsk,
		}

		if len(messages) > 0 && r.notifier != nil {
			msg := strings.Join(messages, "\n\n")
			if err := r.notifier.Notify(ctx, msg); err != nil {
				fmt.Println("[notify error]:", err)
			}
		}
	}

	return nil
}

// Check делает минимальную проверку, что runner может получить данные по первому айтему.
// Отдельный PingUserTargets всё ещё можно держать на уровне DMarket-клиента в main.go.
func (r *DMarketTargetRunner) Check(ctx context.Context) error {
	if r.market == nil {
		return fmt.Errorf("market data adapter is nil")
	}

	if len(r.cfg.Items) == 0 {
		return fmt.Errorf("config has no items")
	}

	it := r.cfg.Items[0]
	topN := it.TopN
	if topN <= 0 {
		topN = 1
	}

	_, err := r.market.DepthByTitle(ctx, it.GameID, it.Title, topN)
	if err != nil {
		return fmt.Errorf("depth by title: %w", err)
	}

	return nil
}

func (r *DMarketTargetRunner) resolveItems(ctx context.Context) ([]domain.TargetItem, error) {
	if !r.cfg.AccountTargets.Enabled {
		return r.cfg.Items, nil
	}

	if r.targetSource == nil {
		return nil, fmt.Errorf("account targets enabled, but target source is nil")
	}

	// overrides из YAML по ключу gameID|title
	overrides := make(map[string]domain.TargetItem)
	for _, it := range r.cfg.Items {
		overrides[it.GameID+"|"+it.Title] = it
	}

	var result []domain.TargetItem

	for _, gameID := range r.cfg.AccountTargets.GameIDs {
		targets, err := r.targetSource.ListUserTargets(
			ctx,
			gameID,
			r.cfg.AccountTargets.Statuses,
		)
		if err != nil {
			return nil, err
		}

		for _, t := range targets {
			item := domain.TargetItem{
				Title:       t.Title,
				GameID:      t.GameID,
				MyTargetUSD: t.PriceUSD,
				TopN:        r.cfg.AccountTargets.DefaultTopN,
			}

			if item.TopN <= 0 {
				item.TopN = 5
			}

			key := item.GameID + "|" + item.Title
			if override, ok := overrides[key]; ok {
				if override.LowAskAlertUSD > 0 {
					item.LowAskAlertUSD = override.LowAskAlertUSD
				}
				if override.TopN > 0 {
					item.TopN = override.TopN
				}
			}

			result = append(result, item)
		}
	}

	return result, nil
}
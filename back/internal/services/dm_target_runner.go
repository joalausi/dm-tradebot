package services

import (
	"context"
	"fmt"
	"strings"
	"time"

	"back/internal/config"
	"back/internal/domain"
	"back/internal/ports"
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

	items, err := r.resolveItems(ctx)
	if err != nil {
		return fmt.Errorf("resolve items: %w", err)
	}

	now := time.Now().Format("2006-01-02 15:04:05")
	fmt.Println()
	fmt.Println("========== DMarket Watch @", now, "==========")

	if len(items) == 0 {
		fmt.Println("[INFO] no targets to watch")
		return nil
	}

	for _, it := range items {
		topN := it.TopN
		if topN <= 0 {
			topN = 5
		}

		fmt.Println()
		fmt.Println("================================================================")

		depth, err := r.market.DepthByTarget(ctx, it, topN)
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

		if domain.HasAdvancedAttributes(it.Attributes) {
			fmt.Printf("attrs: %s\n", domain.PrettyTargetAttributes(it.Attributes))
			fmt.Println("[INFO] advanced target: matching bids by attributes")
		}

		if it.TargetID != "" || it.Status != "" || it.Amount > 0 {
			fmt.Printf("account target: id=%s status=%s amount=%d\n", it.TargetID, it.Status, it.Amount)
		}

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

		key := domain.TargetKey(it.GameID, it.Title, it.Attributes)
		prev := r.state[key]

		var messages []string

		attrNote := ""
		if domain.HasAdvancedAttributes(it.Attributes) {
			attrNote = "\nAttrs: " + domain.PrettyTargetAttributes(it.Attributes)
		}

		if undercut && !prev.Undercut {
			messages = append(messages,
				fmt.Sprintf(
					"⚠️ Твой таргет перебит\n%s%s\nBestTarget: %.2f (твой %.2f)",
					it.Title,
					attrNote,
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
					"💡 Низкий оффер\n%s%s\nBestOffer: %.2f (порог %.2f)",
					it.Title,
					attrNote,
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

	items, err := r.resolveItems(ctx)
	if err != nil {
		return fmt.Errorf("resolve items: %w", err)
	}

	if len(items) == 0 {
		return fmt.Errorf("no items to check")
	}

	it := items[0]
	topN := it.TopN
	if topN <= 0 {
		topN = 1
	}

	_, err = r.market.DepthByTarget(ctx, it, topN)
	if err != nil {
		return fmt.Errorf("depth by target: %w", err)
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

	overrides := make(map[string]domain.TargetItem)

	for _, it := range r.cfg.Items {
		exactKey := domain.TargetKey(it.GameID, it.Title, it.Attributes)
		overrides[exactKey] = it
	}

	var result []domain.TargetItem

	for _, gameID := range r.cfg.AccountTargets.GameIDs {
		targets, err := r.targetSource.ListUserTargets(
			ctx,
			gameID,
			r.cfg.AccountTargets.Statuses,
		)
		if err != nil {
			return nil, fmt.Errorf("list user targets for game %s: %w", gameID, err)
		}

		for _, t := range targets {
			topN := r.cfg.AccountTargets.DefaultTopN
			if topN <= 0 {
				topN = 5
			}

			item := domain.TargetItem{
				Title:       t.Title,
				GameID:      t.GameID,
				MyTargetUSD: t.PriceUSD,
				TopN:        topN,

				TargetID:   t.TargetID,
				Status:     t.Status,
				Amount:     t.Amount,
				Attributes: t.Attributes,
			}

			exactKey := domain.TargetKey(item.GameID, item.Title, item.Attributes)
			titleOnlyKey := domain.TargetKey(item.GameID, item.Title, nil)

			if override, ok := overrides[exactKey]; ok {
				applyTargetOverride(&item, override)
			} else if override, ok := overrides[titleOnlyKey]; ok {
				applyTargetOverride(&item, override)
			}

			result = append(result, item)
		}
	}

	return result, nil
}

func applyTargetOverride(item *domain.TargetItem, override domain.TargetItem) {
	if override.LowAskAlertUSD > 0 {
		item.LowAskAlertUSD = override.LowAskAlertUSD
	}

	if override.TopN > 0 {
		item.TopN = override.TopN
	}

	// DMarket /user-targets remains the source of truth for target state.
}

func (r *DMarketTargetRunner) CurrentTargets(ctx context.Context) ([]domain.TargetView, error) {
	items, err := r.resolveItems(ctx)
	if err != nil {
		return nil, fmt.Errorf("resolve items: %w", err)
	}

	out := make([]domain.TargetView, 0, len(items))

	for _, it := range items {
		topN := it.TopN
		if topN <= 0 {
			topN = 5
		}

		depth, err := r.market.DepthByTitle(ctx, it.GameID, it.Title, topN)
		if err != nil {
			return nil, fmt.Errorf("depth %s: %w", it.Title, err)
		}

		maxTarget := 0.0
		targetQtyTotal := 0
		if len(depth.Bids) > 0 {
			maxTarget = depth.Bids[0].Price
			targetQtyTotal = depth.Bids[0].Qty
		}

		bestOffer := 0.0
		if len(depth.Asks) > 0 {
			bestOffer = depth.Asks[0].Price
		}

		roiUSD := 0.0
		roiPercent := 0.0

		if it.MyTargetUSD > 0 && bestOffer > 0 {
			roiUSD = bestOffer - it.MyTargetUSD
			roiPercent = roiUSD / it.MyTargetUSD * 100
		}

		out = append(out, domain.TargetView{
			Title:           it.Title,
			GameID:          it.GameID,
			MyTargetUSD:    it.MyTargetUSD,
			MaxTargetUSD:   maxTarget,
			BestOfferUSD:   bestOffer,
			MyQty:          1,
			TargetQtyTotal: targetQtyTotal,
			ROIUSD:         roiUSD,
			ROIPercent:     roiPercent,
			IsActive:       true,
			IsOutbid:       maxTarget > it.MyTargetUSD,
		})
	}

	return out, nil
}

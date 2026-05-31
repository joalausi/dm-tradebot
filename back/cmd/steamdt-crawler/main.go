package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"sort"
	"syscall"
	"time"

	"back/internal/adapters/markets/steamdt"
	"back/internal/config"
	"back/internal/services"
	"back/internal/storage/sqlite"
)

func main() {
	cfgPath := flag.String("config", "config.steamdt.yaml", "path to steamdt config yaml")
	check := flag.Bool("check", false, "check SteamDT connectivity and exit")
	once := flag.Bool("once", false, "run single request and print formatted response")
	raw := flag.Bool("raw", false, "print raw JSON responses")
	flag.Parse()

	if !*check && !*once {
		*once = true
	}

	cfg, err := config.LoadSteamDTSmokeConfig(*cfgPath)
	if err != nil {
		log.Fatalf("load config: %v", err)
	}

	watchlist := cfg.ResolveWatchlist()
	if len(watchlist) == 0 {
		log.Fatal("watchlist is empty")
	}

	client, err := steamdt.NewClient()
	if err != nil {
		log.Fatalf("steamdt client: %v", err)
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	db, err := sqlite.Open(cfg.Database.Path)
	if err != nil {
		log.Fatalf("open sqlite: %v", err)
	}
	defer db.Close()

	if err := sqlite.Migrate(ctx, db); err != nil {
		log.Fatalf("migrate sqlite: %v", err)
	}

	snapshotRepo := sqlite.NewSnapshotRepository(db)
	anomalyRepo := sqlite.NewAnomalyRepository(db)
	detector := services.NewSteamDTAnomalyDetector(cfg, anomalyRepo)

	switch {
	case *check:
		resp, err := client.FetchPriceSingle(ctx, watchlist[0])
		if err != nil {
			log.Fatalf("steamdt check failed: %v", err)
		}

		fmt.Printf("SteamDT API looks good ✅ item=%q platforms=%d\n", watchlist[0], len(resp.Data))
		if len(resp.Data) > 0 {
			first := resp.Data[0]
			fmt.Printf(
				"first platform=%s sellPrice=%.2f sellCount=%d bidPrice=%.2f bidCount=%d updated=%s (%s)\n",
				first.Platform,
				first.SellPrice,
				first.SellCount,
				first.BiddingPrice,
				first.BiddingCount,
				formatUnixTime(first.UpdateTime),
				formatAge(first.UpdateTime),
			)
		}
		return

	case *once:
		results, err := client.FetchMany(ctx, watchlist)
		if err != nil {
			log.Fatalf("steamdt fetch failed: %v", err)
		}
		fetchedAt := time.Now().UTC()

		saved, skipped, err := snapshotRepo.SaveBatch(ctx, results, fetchedAt)
		if err != nil {
			log.Fatalf("save snapshots: %v", err)
		}

		fmt.Printf("saved live snapshots=%d skipped stale snapshots=%d\n", saved, skipped)

		alerts, err := detector.Detect(ctx, results, fetchedAt)
		if err != nil {
			log.Fatalf("detect anomalies: %v", err)
		}

		if err := anomalyRepo.SaveAlerts(ctx, alerts); err != nil {
			log.Fatalf("save alerts: %v", err)
		}

		if *raw {
			out, err := json.MarshalIndent(results, "", "  ")
			if err != nil {
				log.Fatalf("marshal response: %v", err)
			}
			fmt.Println(string(out))
			return
		}

		printAlerts(alerts)
		printSummary(results)
		return
	}
}

func printSummary(results map[string]steamdt.PriceSingleResponse) {
	names := make([]string, 0, len(results))
	for name := range results {
		names = append(names, name)
	}
	sort.Strings(names)

	fmt.Println("========== SteamDT Watch ==========")
	for _, name := range names {
		resp := results[name]

		fmt.Printf("\n%s\n", name)
		fmt.Println("--------------------------------------------------------------------------")
		fmt.Printf("%-10s | %-10s | %-10s | %-10s | %-24s\n", "PLATFORM", "SELL", "SELL_CNT", "BID_CNT", "UPDATED")
		fmt.Println("--------------------------------------------------------------------------")

		if len(resp.Data) == 0 {
			fmt.Printf("%-10s | %-10s | %-10s | %-10s | %-24s\n", "-", "-", "-", "-", "-")
			continue
		}

		liveCount := 0
		for _, p := range resp.Data {
			if isProbablyStalePlatform(p) {
				continue
			}

			updated := fmt.Sprintf("%s (%s)", formatUnixTime(p.UpdateTime), formatAge(p.UpdateTime))
			fmt.Printf(
				"%-10s | %-10.2f | %-10d | %-10d | %-24s\n",
				p.Platform,
				p.SellPrice,
				p.SellCount,
				p.BiddingCount,
				updated,
			)
			liveCount++
		}

		if liveCount == 0 {
			fmt.Printf("%-10s | %-10s | %-10s | %-10s | %-24s\n", "NO_DATA", "-", "-", "-", "-")
		}
	}
}

// ------------      Helpers  ----------------------------------
func formatUnixTime(ts int64) string {
	if ts <= 0 {
		return "-"
	}
	return time.Unix(ts, 0).In(time.Local).Format("2006-01-02 15:04:05 MST")
}

func formatAge(ts int64) string {
	if ts <= 0 {
		return "-"
	}

	d := time.Since(time.Unix(ts, 0))
	if d < 0 {
		d = 0
	}

	switch {
	case d < time.Minute:
		return fmt.Sprintf("%ds ago", int(d.Seconds()))
	case d < time.Hour:
		return fmt.Sprintf("%dm ago", int(d.Minutes()))
	case d < 24*time.Hour:
		return fmt.Sprintf("%dh ago", int(d.Hours()))
	default:
		return fmt.Sprintf("%dd ago", int(d.Hours()/24))
	}
}

func isProbablyStalePlatform(p steamdt.PlatformPrice) bool {
	if p.SellPrice == 0 &&
		p.SellCount == 0 &&
		p.BiddingPrice == 0 &&
		p.BiddingCount == 0 {
		return true
	}
	return false
}

func printAlerts(alerts []sqlite.AnomalyAlert) {
	if len(alerts) == 0 {
		fmt.Println("\nNo anomalies detected.")
		return
	}

	fmt.Println("\n========== SteamDT Alerts ==========")
	for _, a := range alerts {
		fmt.Printf(
			"%s | %s | %s | current=%.0f baseline=%.0f change=%.2f%%\n",
			a.MarketHashName,
			a.Platform,
			a.Metric,
			a.CurrentValue,
			a.BaselineValue,
			a.PctChange,
		)
	}
}

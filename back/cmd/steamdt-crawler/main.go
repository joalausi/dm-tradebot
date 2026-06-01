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
	"back/internal/storage/sqlite"
)

func main() {
	cfgPath := flag.String("config", "config.steamdt.yaml", "path to steamdt config yaml")
	check := flag.Bool("check", false, "check SteamDT connectivity and exit")
	once := flag.Bool("once", false, "run single request and print formatted response")
	raw := flag.Bool("raw", false, "print raw JSON responses")
	syncBase := flag.Bool("sync-base", false, "sync SteamDT base catalog and exit")
	collectCatalogChunk := flag.Bool("collect-catalog-chunk", false, "collect next catalog chunk and save snapshots")
	flag.Parse()

	if !*check && !*once && !*syncBase && !*collectCatalogChunk {
		*once = true
	}

	cfg, err := config.LoadSteamDTSmokeConfig(*cfgPath)
	if err != nil {
		log.Fatalf("load config: %v", err)
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

	catalogRepo := sqlite.NewCatalogRepository(db)
	stateRepo := sqlite.NewCollectorStateRepository(db)
	snapshotRepo := sqlite.NewSnapshotRepository(db)

	switch {
	case *syncBase:
		lastSyncedAt, err := catalogRepo.LastSyncedAt(ctx)
		if err != nil {
			log.Fatalf("catalog last synced: %v", err)
		}

		if lastSyncedAt > 0 {
			age := time.Since(time.Unix(lastSyncedAt, 0))
			if age < time.Duration(cfg.Catalog.SyncTTLHours)*time.Hour {
				fmt.Printf("catalog is fresh enough, skip sync (last synced %s ago)\n", age.Round(time.Minute))
				return
			}
		}

		baseResp, err := client.FetchBase(ctx)
		if err != nil {
			log.Fatalf("sync base failed: %v", err)
		}

		syncedAt := time.Now().UTC()
		if err := catalogRepo.ReplaceAll(ctx, baseResp.Data, syncedAt); err != nil {
			log.Fatalf("save catalog: %v", err)
		}

		fmt.Printf("catalog synced: items=%d at=%s\n", len(baseResp.Data), syncedAt.Format(time.RFC3339))
		return

	case *collectCatalogChunk:
		total, err := catalogRepo.Count(ctx)
		if err != nil {
			log.Fatalf("catalog count: %v", err)
		}
		if total == 0 {
			log.Fatal("catalog is empty, run -sync-base first")
		}

		offset, err := stateRepo.GetInt(ctx, "collector_offset", 0)
		if err != nil {
			log.Fatalf("read collector state: %v", err)
		}

		for i := 0; i < cfg.Collector.MaxChunksPerRun; i++ {
			chunk, err := catalogRepo.ListChunk(ctx, cfg.Collector.BatchSize, offset)
			if err != nil {
				log.Fatalf("list catalog chunk: %v", err)
			}

			if len(chunk) == 0 {
				offset = 0
				if err := stateRepo.SetInt(ctx, "collector_offset", offset); err != nil {
					log.Fatalf("reset collector state: %v", err)
				}
				fmt.Println("catalog chunk empty, offset reset to 0")
				return
			}

			names := make([]string, 0, len(chunk))
			for _, item := range chunk {
				names = append(names, item.MarketHashName)
			}

			results, err := client.FetchMany(ctx, names)
			if err != nil {
				log.Fatalf("fetch catalog chunk: %v", err)
			}

			fetchedAt := time.Now().UTC()
			saved, skipped, err := snapshotRepo.SaveBatch(ctx, results, fetchedAt)
			if err != nil {
				log.Fatalf("save snapshots: %v", err)
			}

			fmt.Printf(
				"chunk=%d/%d offset=%d size=%d saved=%d skipped=%d\n",
				i+1,
				cfg.Collector.MaxChunksPerRun,
				offset,
				len(chunk),
				saved,
				skipped,
			)

			offset += len(chunk)
			if offset >= total {
				offset = 0
			}

			if err := stateRepo.SetInt(ctx, "collector_offset", offset); err != nil {
				log.Fatalf("update collector state: %v", err)
			}
		}
		return

	case *check:
		watchlist := cfg.ResolveWatchlist()
		if len(watchlist) == 0 {
			log.Fatal("watchlist is empty")
		}

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
		watchlist := cfg.ResolveWatchlist()
		if len(watchlist) == 0 {
			log.Fatal("watchlist is empty")
		}
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

		if *raw {
			out, err := json.MarshalIndent(results, "", "  ")
			if err != nil {
				log.Fatalf("marshal response: %v", err)
			}
			fmt.Println(string(out))
			return
		}

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

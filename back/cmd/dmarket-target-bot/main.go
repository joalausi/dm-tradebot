package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"strings"
	"syscall"

	"back/internal/adapters/httpapi"
	"back/internal/adapters/markets/dmarket"
	"back/internal/adapters/notifiers/console"
	"back/internal/adapters/notifiers/discord"
	"back/internal/config"
	"back/internal/ports"
	"back/internal/services"
	"back/internal/storage/sqlite"
)

func main() {
	cfgPath := flag.String("config", "config.yaml", "path to config yaml")
	apiAddr := flag.String("api", "", "HTTP API listen address, example: :8080")
	once := flag.Bool("once", false, "run single iteration and exit")
	check := flag.Bool("check", false, "check DMarket connectivity and exit")
	crawl := flag.Bool("crawl-dmarket", false, "run DMarket market crawler once and exit")
	apiOnly := flag.Bool("api-only", false, "run HTTP API and DMarket crawler without target monitor")
	flag.Parse()

	cfg, err := config.LoadFromFile(*cfgPath)
	if err != nil {
		log.Fatalf("load config: %v", err)
	}

	dmClient, err := dmarket.NewClient()
	if err != nil {
		log.Fatalf("dmarket client: %v", err)
	}

	var market ports.MarketData = dmClient
	var targetSource ports.UserTargets
	if cfg.AccountTargets.Enabled {
		targetSource = dmClient
	}

	consoleNotifier := console.New()
	discordNotifier := discord.New(cfg.Discord)

	var notifier ports.Notifier
	if discordNotifier != nil {
		notifier = console.Multi{
			Notifiers: []ports.Notifier{
				consoleNotifier,
				discordNotifier,
			},
		}
	} else {
		notifier = consoleNotifier
	}
	runner := services.NewDMarketTargetRunner(cfg, market, notifier, targetSource)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	var opportunityStore *sqlite.OpportunityRepository
	var opportunityFilter ports.OpportunityFilter
	var crawler *services.DMarketMarketCrawler

	if cfg.DMarketCrawler.Enabled || *apiAddr != "" || *crawl {
		db, err := sqlite.Open(cfg.DMarketCrawler.SteamDTFilter.DatabasePath)
		if err != nil {
			log.Fatalf("open shared sqlite: %v", err)
		}
		defer db.Close()

		if err := sqlite.Migrate(ctx, db); err != nil {
			log.Fatalf("migrate shared sqlite: %v", err)
		}

		opportunityStore = sqlite.NewOpportunityRepository(db)

		if cfg.DMarketCrawler.SteamDTFilter.Enabled {
			filter, err := services.NewSteamDTOpportunityFilter(
				cfg.DMarketCrawler.SteamDTFilter,
				sqlite.NewSteamDTSignalReader(db),
			)
			if err != nil {
				log.Fatalf("configure SteamDT opportunity filter: %v", err)
			}
			opportunityFilter = filter
		}

		crawler = services.NewDMarketMarketCrawler(
			cfg.DMarketCrawler,
			dmClient,
			dmClient,
			opportunityStore,
			opportunityFilter,
		)
	}

	apiErr := make(chan error, 1)
	if *apiAddr != "" {
		apiServer := httpapi.New(
			*apiAddr,
			runner,
			opportunityStore,
			splitCSV(os.Getenv("DMTARGETBOT_ALLOWED_USERS")),
		)

		go func() {
			apiErr <- apiServer.Run(ctx)
		}()
	}

	switch {
	case *crawl:
		if crawler == nil {
			log.Fatal("dmarket crawler is not configured")
		}
		if err := crawler.RunOnce(ctx); err != nil {
			log.Fatalf("dmarket crawler: %v", err)
		}

		return

	case *apiOnly:
		if *apiAddr == "" {
			log.Fatal("-api-only requires -api")
		}
		if crawler != nil {
			go func() {
				if err := crawler.Run(ctx); err != nil && err != context.Canceled {
					log.Printf("dmarket crawler stopped: %v", err)
				}
			}()
		}

		select {
		case <-ctx.Done():
			return
		case err := <-apiErr:
			if err != nil {
				log.Fatalf("api server: %v", err)
			}
			return
		}

	case *check:
		if err := dmClient.PingUserTargets(ctx); err != nil {
			log.Fatalf("check failed: ping user-targets: %v", err)
		}

		if err := runner.Check(ctx); err != nil {
			log.Fatalf("check failed: %v", err)
		}

		fmt.Println("Connection to DMarket API looks good ✅")
		return

	case *once:
		if err := runner.RunOnce(ctx); err != nil {
			log.Fatalf("run once: %v", err)
		}
		return

	default:
		if crawler != nil {
			go func() {
				if err := crawler.Run(ctx); err != nil && err != context.Canceled {
					log.Printf("dmarket crawler stopped: %v", err)
				}
			}()
		}
		if err := runner.Run(ctx); err != nil && err != context.Canceled {
			log.Fatalf("run: %v", err)
		}
	}
}

func splitCSV(value string) []string {
	parts := strings.Split(value, ",")
	out := make([]string, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if part != "" {
			out = append(out, part)
		}
	}
	return out
}

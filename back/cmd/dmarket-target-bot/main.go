package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"back/internal/adapters/markets/dmarket"
	"back/internal/config"
	"back/internal/domain"
	"back/internal/ports"
	"back/internal/services"
	"back/internal/adapters/notifiers/console"
	"back/internal/adapters/notifiers/dmarket"
)

func main() {
	cfgPath := flag.String("config", "config.yaml", "path to config yaml")
	once := flag.Bool("once", false, "run single iteration and exit")
	check := flag.Bool("check", false, "check DMarket connectivity and exit")
	flag.Parse()

	cfg, err := config.LoadConfigFromFile(*cfgPath)
	if err != nil {
		log.Fatalf("load config: %v", err)
	}

	dmClient, err := dmarket.NewClient()
	if err != nil {
		log.Fatalf("dmarket client: %v", err)
	}

	var market dmarket.MarketData = dmClient

	console := console.Notifier{}
	disc := dmarket.NewDiscordNotifier(cfg.Discord)

	var notifier dmarket.Notifier
	if disc != nil {
		// both
		notifier = dmarket.MultiNotifier{Notifiers: []dmarket.Notifier{console, disc}}
	} else {
		//console only
		notifier = console
	}

	r := services.NewDMarketTargetRunner(cfg, market, notifier)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	switch {
	case *check:
		if err := dmClient.PingUserTargets(ctx); err != nil {
			log.Fatalf("check failed: ping user-targets: %v", err)
		}
		fmt.Println("Connection to DMarket API looks good ✅")
		return
	case *once:
		if err := r.RunOnce(ctx); err != nil {
			log.Fatalf("run once: %v", err)
		}
	default:
		if err := r.Run(ctx); err != nil && err != context.Canceled {
			log.Fatalf("run: %v", err)
		}
	}
}

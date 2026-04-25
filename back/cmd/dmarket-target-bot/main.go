package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"back/internal/adapters"
	"back/internal/core"
)

func main() {
	cfgPath := flag.String("config", "config.yaml", "path to config yaml")
	once := flag.Bool("once", false, "run single iteration and exit")
	check := flag.Bool("check", false, "check DMarket connectivity and exit")
	flag.Parse()

	cfg, err := core.LoadConfigFromFile(*cfgPath)
	if err != nil {
		log.Fatalf("load config: %v", err)
	}
	
	dmClient, err := adapters.NewDMarketClient()
	if err != nil {
		log.Fatalf("dmarket client: %v", err)
	}

	var market core.MarketData = dmClient

	console := core.ConsoleNotifier{}
	disc := adapters.NewDiscordNotifier(cfg.Discord)

	var notifier core.Notifier
	if disc != nil {
		// both
		notifier = core.MultiNotifier{Notifiers: []core.Notifier{console, disc}}
	} else {
		//console only
		notifier = console
	}

	r := core.NewRunner(cfg, market, notifier)

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

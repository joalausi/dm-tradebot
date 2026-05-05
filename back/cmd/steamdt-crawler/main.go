package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"

	"back/internal/adapters/markets/steamdt"
	"back/internal/config"
)

func main() {
	cfgPath := flag.String("config", "config.steamdt.yaml", "path to steamdt config yaml")
	check := flag.Bool("check", false, "check SteamDT connectivity and exit")
	once := flag.Bool("once", false, "run single request and print response")
	flag.Parse()

	if !*check && !*once {
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

	switch {
	case *check:
		resp, err := client.FetchPriceSingle(ctx, cfg.MarketHashName)
		if err != nil {
			log.Fatalf("steamdt check failed: %v", err)
		}

		fmt.Printf("SteamDT API looks good ✅ marketHashName=%q platforms=%d\n", cfg.MarketHashName, len(resp.Data))
		if len(resp.Data) > 0 {
			first := resp.Data[0]
			fmt.Printf(
				"first platform=%s sellPrice=%.2f sellCount=%d bidPrice=%.2f bidCount=%d updateTime=%d\n",
				first.Platform,
				first.SellPrice,
				first.SellCount,
				first.BiddingPrice,
				first.BiddingCount,
				first.UpdateTime,
			)
		}
		return

	case *once:
		resp, err := client.FetchPriceSingle(ctx, cfg.MarketHashName)
		if err != nil {
			log.Fatalf("steamdt fetch failed: %v", err)
		}

		out, err := json.MarshalIndent(resp, "", "  ")
		if err != nil {
			log.Fatalf("marshal response: %v", err)
		}

		fmt.Println(string(out))
		return
	}
}
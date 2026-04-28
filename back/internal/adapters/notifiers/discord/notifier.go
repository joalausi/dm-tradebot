package discord

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"back/internal/config"
	"back/internal/ports"
)

type Notifier struct {
	cfg config.DiscordConfig
}

func New(cfg config.DiscordConfig) ports.Notifier {
	if cfg.WebhookURL == "" {
		return nil
	}
	return &Notifier{cfg: cfg}
}

func (d *Notifier) Notify(ctx context.Context, msg string) error {
	if d.cfg.Mention != "" {
		msg = d.cfg.Mention + " " + msg
	}
	payload := map[string]any{
		"content": msg,
		"allowed_mentions": map[string]any{
			"parse": []string{},
		},
	}
	b, _ := json.Marshal(payload)
	req, _ := http.NewRequestWithContext(ctx, http.MethodPost, d.cfg.WebhookURL, bytes.NewReader(b))
	req.Header.Set("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return fmt.Errorf("discord %d", resp.StatusCode)
	}
	return nil
}

// TODO: DONE

package adapters

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"back/internal/core"
)

type DiscordNotifier struct {
	cfg core.DiscordConfig
}

func NewDiscordNotifier(cfg core.DiscordConfig) core.Notifier {
	if cfg.WebhookURL == "" {
		return nil
	}
	return &DiscordNotifier{cfg: cfg}
}

func (d *DiscordNotifier) Notify(ctx context.Context, msg string) error {
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

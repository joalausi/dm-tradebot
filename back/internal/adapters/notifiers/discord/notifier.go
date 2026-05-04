package discord

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"strings"

	"back/internal/config"
	"back/internal/ports"
)

type Notifier struct {
	webhookURL string
	mention    string
}

func New(cfg config.DiscordConfig) ports.Notifier {
	webhookURL := strings.TrimSpace(os.Getenv("DISCORD_WEBHOOK_URL"))
	if webhookURL == "" {
		return nil
	}

	return &Notifier{
		webhookURL: webhookURL,
		mention:    cfg.Mention,
	}
}

func (d *Notifier) Notify(ctx context.Context, msg string) error {
	if d.mention != "" {
		msg = d.mention + " " + msg
	}

	payload := map[string]any{
		"content": msg,
		"allowed_mentions": map[string]any{
			"parse": []string{},
		},
	}

	b, _ := json.Marshal(payload)

	req, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		d.webhookURL,
		bytes.NewReader(b),
	)
	if err != nil {
		return err
	}

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

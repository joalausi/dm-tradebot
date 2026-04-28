package console

import (
	"context"
	"fmt"

	"back/internal/ports"
)

type Notifier struct{}

func New() ports.Notifier {
	return Notifier{}
}

func (Notifier) Notify(ctx context.Context, msg string) error {
	fmt.Println("[NOTIFY]", msg)
	return nil
}

type Multi struct {
	Notifiers []ports.Notifier
}

func (m Multi) Notify(ctx context.Context, msg string) error {
	for _, n := range m.Notifiers {
		_ = n.Notify(ctx, msg)
	}
	return nil
}

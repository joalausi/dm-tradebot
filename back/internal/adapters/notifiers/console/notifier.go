package console

import (
	"context"
	"errors"
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
	var errs []error

	for _, n := range m.Notifiers {
		if n == nil {
			continue
		}

		if err := n.Notify(ctx, msg); err != nil {
			errs = append(errs, fmt.Errorf("%T: %w", n, err))
		}
	}

	return errors.Join(errs...)
}

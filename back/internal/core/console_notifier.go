package core

import (
	"context"
	"fmt"
)

// ConsoleNotifier просто печатает текст — удобно для MVP и тестов.
type ConsoleNotifier struct{}

func (ConsoleNotifier) Notify(ctx context.Context, msg string) error {
	fmt.Println("[NOTIFY]", msg)
	return nil
}

// MultiNotifier можно использовать, чтобы слать и в консоль, и в Discord.
type MultiNotifier struct {
	Notifiers []Notifier
}

func (m MultiNotifier) Notify(ctx context.Context, msg string) error {
	for _, n := range m.Notifiers {
		_ = n.Notify(ctx, msg)
	}
	return nil
}

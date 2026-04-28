package ports

import (
	"context"

	"back/internal/domain"
)

type UserTargets interface {
	ListUserTargets(ctx context.Context, gameID string, statuses []string) ([]domain.UserTarget, error)
}

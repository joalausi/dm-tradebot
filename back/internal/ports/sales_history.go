package ports

import (
	"context"

	"back/internal/domain"
)

type SalesHistory interface {
	LastSales(ctx context.Context, gameID, title string, limit int) ([]domain.Sale, error)
}

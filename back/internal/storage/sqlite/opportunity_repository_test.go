package sqlite

import (
	"context"
	"path/filepath"
	"testing"
	"time"

	"back/internal/domain"
)

func TestOpportunityRepositoryReplaceAndRead(t *testing.T) {
	ctx := context.Background()
	db, err := Open(filepath.Join(t.TempDir(), "test.db"))
	if err != nil {
		t.Fatal(err)
	}
	defer db.Close()
	if err := Migrate(ctx, db); err != nil {
		t.Fatal(err)
	}

	repo := NewOpportunityRepository(db)
	now := time.Date(2026, 7, 26, 12, 0, 0, 0, time.UTC)
	items := []domain.Opportunity{
		{GameID: "a8db", Title: "B", ScannedAt: now, Score: 10, SteamDT: domain.SteamDTFilterResult{Eligible: true}},
		{GameID: "a8db", Title: "A", ScannedAt: now, Score: 20, SteamDT: domain.SteamDTFilterResult{Eligible: true}},
	}
	if err := repo.SaveOpportunities(ctx, items); err != nil {
		t.Fatal(err)
	}

	got, err := repo.CurrentOpportunities(ctx)
	if err != nil {
		t.Fatal(err)
	}
	if len(got) != 2 || got[0].Title != "A" || got[1].Title != "B" {
		t.Fatalf("unexpected opportunities: %#v", got)
	}

	if err := repo.SaveOpportunities(ctx, nil); err != nil {
		t.Fatal(err)
	}
	got, err = repo.CurrentOpportunities(ctx)
	if err != nil {
		t.Fatal(err)
	}
	if len(got) != 0 {
		t.Fatalf("empty replacement retained %d rows", len(got))
	}
	if got == nil {
		t.Fatal("empty replacement returned a nil slice")
	}
}

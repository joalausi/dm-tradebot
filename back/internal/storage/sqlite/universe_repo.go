package sqlite

import (
	"context"
	"database/sql"
	"encoding/json"
	"strings"
	"time"

	"back/internal/adapters/markets/steamdt"
	"back/internal/config"
)

type UniverseRepository struct {
	db *sql.DB
}

func NewUniverseRepository(db *sql.DB) *UniverseRepository {
	return &UniverseRepository{db: db}
}

func (r *UniverseRepository) RebuildPlatformsFromCatalog(ctx context.Context) (int, error) {
	rows, err := r.db.QueryContext(ctx, `
		select raw_json, synced_at_unix
		from steamdt_catalog_items
	`)
	if err != nil {
		return 0, err
	}
	defer rows.Close()

	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return 0, err
	}
	defer tx.Rollback()

	if _, err := tx.ExecContext(ctx, `delete from steamdt_catalog_platforms`); err != nil {
		return 0, err
	}

	stmt, err := tx.PrepareContext(ctx, `
		insert into steamdt_catalog_platforms (
			market_hash_name,
			platform,
			platform_item_id,
			synced_at_unix
		) values (?, ?, ?, ?)
		on conflict(market_hash_name, platform) do update set
			platform_item_id = excluded.platform_item_id,
			synced_at_unix = excluded.synced_at_unix
	`)
	if err != nil {
		return 0, err
	}
	defer stmt.Close()

	inserted := 0

	for rows.Next() {
		var raw string
		var syncedAtUnix int64

		if err := rows.Scan(&raw, &syncedAtUnix); err != nil {
			return 0, err
		}

		var item steamdt.BaseItem
		if err := json.Unmarshal([]byte(raw), &item); err != nil {
			return 0, err
		}

		for _, p := range item.PlatformList {
			platform := strings.ToUpper(strings.TrimSpace(p.Name))
			if platform == "" {
				continue
			}

			if _, err := stmt.ExecContext(
				ctx,
				item.MarketHashName,
				platform,
				p.ItemID,
				syncedAtUnix,
			); err != nil {
				return 0, err
			}
			inserted++
		}
	}

	if err := rows.Err(); err != nil {
		return 0, err
	}

	if err := tx.Commit(); err != nil {
		return 0, err
	}

	return inserted, nil
}

func (r *UniverseRepository) RebuildWorkingUniverse(
	ctx context.Context,
	cfg config.SteamDTSmokeConfig,
) (enabled int, disabled int, err error) {
	rows, err := r.db.QueryContext(ctx, `
		select market_hash_name
		from steamdt_catalog_items
		order by market_hash_name asc
	`)
	if err != nil {
		return 0, 0, err
	}
	defer rows.Close()

	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return 0, 0, err
	}
	defer tx.Rollback()

	if _, err := tx.ExecContext(ctx, `delete from steamdt_working_universe`); err != nil {
		return 0, 0, err
	}

	stmt, err := tx.PrepareContext(ctx, `
		insert into steamdt_working_universe (
			market_hash_name,
			enabled,
			reason,
			platform_count,
			has_steam,
			has_buff,
			has_youpin,
			has_c5,
			has_haloskins,
			is_stattrak,
			is_souvenir,
			is_graffiti,
			is_sticker,
			is_patch,
			is_music_kit,
			is_case,
			is_capsule,
			updated_at_unix
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
	`)
	if err != nil {
		return 0, 0, err
	}
	defer stmt.Close()

	now := time.Now().Unix()

	for rows.Next() {
		var marketHashName string
		if err := rows.Scan(&marketHashName); err != nil {
			return 0, 0, err
		}

		platforms, err := r.platformsForMarketHashName(ctx, tx, marketHashName)
		if err != nil {
			return 0, 0, err
		}

		flags := buildUniverseFlags(marketHashName, platforms)

		isEnabled, reason := decideUniverseEnabled(marketHashName, platforms, flags, cfg)

		if isEnabled {
			enabled++
		} else {
			disabled++
		}

		if _, err := stmt.ExecContext(
			ctx,
			marketHashName,
			boolToInt(isEnabled),
			reason,
			len(platforms),
			boolToInt(flags.HasSteam),
			boolToInt(flags.HasBuff),
			boolToInt(flags.HasYoupin),
			boolToInt(flags.HasC5),
			boolToInt(flags.HasHaloSkins),
			boolToInt(flags.IsStatTrak),
			boolToInt(flags.IsSouvenir),
			boolToInt(flags.IsGraffiti),
			boolToInt(flags.IsSticker),
			boolToInt(flags.IsPatch),
			boolToInt(flags.IsMusicKit),
			boolToInt(flags.IsCase),
			boolToInt(flags.IsCapsule),
			now,
		); err != nil {
			return enabled, disabled, err
		}
	}

	if err := rows.Err(); err != nil {
		return 0, 0, err
	}

	if err := tx.Commit(); err != nil {
		return enabled, disabled, err
	}

	return enabled, disabled, nil
}

// --- Helpers----

type universeFlags struct {
	HasSteam     bool
	HasBuff      bool
	HasYoupin    bool
	HasC5        bool
	HasHaloSkins bool

	IsSkinLike bool
	IsAgent    bool
	IsSticker  bool

	IsStatTrak bool
	IsSouvenir bool
	IsGraffiti bool
	IsPatch    bool
	IsMusicKit bool
	IsCase     bool
	IsCapsule  bool
}

func (r *UniverseRepository) platformsForMarketHashName(
	ctx context.Context,
	tx *sql.Tx,
	marketHashName string,
) (map[string]bool, error) {
	rows, err := tx.QueryContext(ctx, `
		select platform
		from steamdt_catalog_platforms
		where market_hash_name = ?
	`, marketHashName)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := make(map[string]bool)
	for rows.Next() {
		var p string
		if err := rows.Scan(&p); err != nil {
			return nil, err
		}
		out[strings.ToUpper(strings.TrimSpace(p))] = true
	}

	return out, rows.Err()
}

func buildUniverseFlags(marketHashName string, platforms map[string]bool) universeFlags {
	name := strings.ToLower(marketHashName)

	return universeFlags{
		HasSteam:     platforms["STEAM"],
		HasBuff:      platforms["BUFF"],
		HasYoupin:    platforms["YOUPIN"],
		HasC5:        platforms["C5"],
		HasHaloSkins: platforms["HALOSKINS"],

		IsSkinLike: hasWearCondition(marketHashName),
		IsAgent:    isAgent(marketHashName),
		IsSticker:  isSticker(marketHashName),

		IsStatTrak: strings.Contains(marketHashName, "StatTrak"),
		IsSouvenir: strings.Contains(marketHashName, "Souvenir"),
		IsGraffiti: strings.Contains(name, "graffiti"),
		IsPatch:    strings.Contains(name, "patch"),
		IsMusicKit: strings.Contains(name, "music kit"),
		IsCase:     strings.Contains(name, "case"),
		IsCapsule:  strings.Contains(name, "capsule"),
	}
}

func hasWearCondition(marketHashName string) bool {
	wearSuffixes := []string{
		"(Factory New)",
		"(Minimal Wear)",
		"(Field-Tested)",
		"(Well-Worn)",
		"(Battle-Scarred)",
	}

	for _, suffix := range wearSuffixes {
		if strings.Contains(marketHashName, suffix) {
			return true
		}
	}

	return false
}

func isSticker(marketHashName string) bool {
	name := strings.ToLower(marketHashName)
	return strings.HasPrefix(name, "sticker |") || strings.HasPrefix(name, "sticker ")
}

func isAgent(marketHashName string) bool {
	if hasWearCondition(marketHashName) {
		return false
	}

	name := strings.ToLower(marketHashName)

	if isSticker(marketHashName) ||
		strings.Contains(name, "graffiti") ||
		strings.Contains(name, "patch") ||
		strings.Contains(name, "music kit") ||
		strings.Contains(name, "case") ||
		strings.Contains(name, "capsule") ||
		strings.Contains(name, "coin") ||
		strings.Contains(name, "medal") ||
		strings.Contains(name, "star for operation") ||
		strings.Contains(name, "stars for operation") {
		return false
	}

	return strings.Contains(marketHashName, " | ")
}

func decideUniverseEnabled(
	marketHashName string,
	platforms map[string]bool,
	flags universeFlags,
	cfg config.SteamDTSmokeConfig,
) (bool, string) {
	if matchesAnyPattern(marketHashName, cfg.Universe.ExcludePatterns) {
		return false, "excluded_by_pattern"
	}

	if !hasAnyRequiredPlatform(platforms, cfg.Universe.RequirePlatforms) {
		return false, "no_required_platform"
	}

	if flags.IsStatTrak && !cfg.Universe.IncludeStatTrak {
		return false, "stattrak_disabled"
	}

	if flags.IsSouvenir && !cfg.Universe.IncludeSouvenir {
		return false, "souvenir_disabled"
	}

	switch {
	case flags.IsSkinLike:
		if !cfg.Universe.IncludeSkins {
			return false, "skins_disabled"
		}
		return true, "ok"

	case flags.IsAgent:
		if !cfg.Universe.IncludeAgents {
			return false, "agents_disabled"
		}
		return true, "ok"

	case flags.IsSticker:
		if !cfg.Universe.IncludeStickers {
			return false, "stickers_disabled"
		}
		return true, "ok"

	default:
		return false, "unsupported_category"
	}
}

func matchesAnyPattern(value string, patterns []string) bool {
	v := strings.ToLower(value)
	for _, p := range patterns {
		p = strings.ToLower(strings.TrimSpace(p))
		if p == "" {
			continue
		}
		if strings.Contains(v, p) {
			return true
		}
	}
	return false
}

func hasAnyRequiredPlatform(platforms map[string]bool, required []string) bool {
	if len(required) == 0 {
		return true
	}

	for _, p := range required {
		p = strings.ToUpper(strings.TrimSpace(p))
		if p == "" {
			continue
		}
		if platforms[p] {
			return true
		}
	}

	return false
}

func boolToInt(v bool) int {
	if v {
		return 1
	}
	return 0
}

func (r *UniverseRepository) CountEnabled(ctx context.Context) (int, error) {
	var n int
	err := r.db.QueryRowContext(ctx, `
		select count(1)
		from steamdt_working_universe
		where enabled = 1
	`).Scan(&n)
	return n, err
}

func (r *UniverseRepository) ListEnabledChunk(
	ctx context.Context,
	limit int,
	offset int,
) ([]string, error) {
	rows, err := r.db.QueryContext(ctx, `
		select market_hash_name
		from steamdt_working_universe
		where enabled = 1
		order by market_hash_name asc
		limit ? offset ?
	`, limit, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []string
	for rows.Next() {
		var name string
		if err := rows.Scan(&name); err != nil {
			return nil, err
		}
		out = append(out, name)
	}

	return out, rows.Err()
}
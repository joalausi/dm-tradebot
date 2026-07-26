package sqlite

const initSQL = `
create table if not exists steamdt_market_snapshots (
  id integer primary key autoincrement,
  market_hash_name text not null,
  platform text not null,
  platform_item_id text not null default '',
  sell_price real not null default 0,
  sell_count integer not null default 0,
  bidding_price real not null default 0,
  bidding_count integer not null default 0,
  update_time_unix integer not null default 0,
  fetched_at_unix integer not null,
  is_stale integer not null default 0
);

create index if not exists idx_steamdt_snapshots_lookup
on steamdt_market_snapshots (market_hash_name, platform, fetched_at_unix);

create index if not exists idx_steamdt_snapshots_time
on steamdt_market_snapshots (fetched_at_unix);

create table if not exists steamdt_catalog_items (
  market_hash_name text primary key,
  name text not null,
  raw_json text not null,
  synced_at_unix integer not null
);

create index if not exists idx_steamdt_catalog_synced
on steamdt_catalog_items (synced_at_unix);

create table if not exists steamdt_catalog_platforms (
  market_hash_name text not null,
  platform text not null,
  platform_item_id text not null default '',
  synced_at_unix integer not null,
  primary key (market_hash_name, platform)
);

create index if not exists idx_steamdt_catalog_platforms_platform
on steamdt_catalog_platforms (platform);

create table if not exists steamdt_working_universe (
  market_hash_name text primary key,
  enabled integer not null default 0,
  reason text not null default '',

  platform_count integer not null default 0,
  has_steam integer not null default 0,
  has_buff integer not null default 0,
  has_youpin integer not null default 0,
  has_c5 integer not null default 0,
  has_haloskins integer not null default 0,

  is_stattrak integer not null default 0,
  is_souvenir integer not null default 0,
  is_graffiti integer not null default 0,
  is_sticker integer not null default 0,
  is_patch integer not null default 0,
  is_music_kit integer not null default 0,
  is_case integer not null default 0,
  is_capsule integer not null default 0,

  updated_at_unix integer not null
);

create index if not exists idx_steamdt_working_universe_enabled
on steamdt_working_universe (enabled);

create index if not exists idx_steamdt_working_universe_reason
on steamdt_working_universe (reason);

create table if not exists steamdt_collector_state (
  state_key text primary key,
  state_value text not null,
  updated_at_unix integer not null
);

create table if not exists steamdt_anomaly_alerts (
  id integer primary key autoincrement,
  market_hash_name text not null,
  platform text not null,
  metric text not null,
  current_value real not null,
  baseline_value real not null,
  pct_change real not null,
  fetched_at_unix integer not null,
  created_at_unix integer not null
);

create index if not exists idx_steamdt_alerts_lookup
on steamdt_anomaly_alerts (market_hash_name, platform, metric, created_at_unix);

create table if not exists dmarket_opportunities (
  game_id text not null,
  title text not null,
  scanned_at_unix integer not null,

  best_target_usd real not null default 0,
  target_count integer not null default 0,
  best_offer_usd real not null default 0,
  offer_count integer not null default 0,

  last_sale_avg_usd real not null default 0,
  last_sale_median_usd real not null default 0,
  last_sales_count integer not null default 0,
  expected_sell_usd real not null default 0,
  gross_profit_usd real not null default 0,
  roi_percent real not null default 0,

  score real not null default 0,
  risk text not null default '',
  reason text not null default '',

  steamdt_eligible integer not null default 0,
  steamdt_reason text not null default '',
  steamdt_universe_known integer not null default 0,
  steamdt_universe_enabled integer not null default 0,
  steamdt_universe_reason text not null default '',
  steamdt_latest_fetched_at_unix integer not null default 0,
  steamdt_snapshot_age_seconds integer not null default 0,
  steamdt_platform_count integer not null default 0,
  steamdt_total_sell_count integer not null default 0,
  steamdt_total_bid_count integer not null default 0,
  steamdt_sell_spike_alerts integer not null default 0,
  steamdt_bid_spike_alerts integer not null default 0,

  primary key (game_id, title)
);

create index if not exists idx_dmarket_opportunities_score
on dmarket_opportunities (score desc, scanned_at_unix desc);
`

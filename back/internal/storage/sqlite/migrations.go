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
`

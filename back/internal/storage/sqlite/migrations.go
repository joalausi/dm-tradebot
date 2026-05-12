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
`
# OptiPlex SteamDT deployment

This Compose project runs a sequential SteamDT worker and the private
DMarket/HTTP API service. The worker refreshes the base catalog and filtered
universe, then collects one configured universe chunk per interval. The API
uses the same SQLite volume to filter DMarket purchase candidates with recent
SteamDT liquidity and anomaly signals.

The API is published only on host loopback (`127.0.0.1:8080`). Tailscale Serve
terminates HTTPS and proxies requests to that loopback listener. Do not publish
the API container on a LAN or public interface.

## Configure

Create the private environment file directly on the server:

```sh
cd /home/artem/dmtargetbot/back/deploy/optiplex
umask 077
read -r -s -p "SteamDT API key: " STEAMDT_API_KEY
printf '\n'
printf 'STEAMDT_API_KEY=%s\nCOLLECT_INTERVAL_SECONDS=600\nCATALOG_REFRESH_SECONDS=86400\nHEALTH_MAX_AGE_SECONDS=2400\n' "$STEAMDT_API_KEY" > .env
unset STEAMDT_API_KEY
```

Review `config.steamdt.yaml`. Persistent state is stored in the named Docker
volume `dmtargetbot-steamdt-data`.

Create the private API environment file separately:

```sh
cd /home/artem/dmtargetbot/back/deploy/optiplex
umask 077
cp env.api.example .env.api
editor .env.api
```

Add the DMarket keys and a comma-separated allowlist of exact Tailscale login
names. Never share one user's DMarket or SSH credentials with another user.

## Start

```sh
docker compose up --detach --build
docker compose ps
docker compose logs --follow --tail=100 steamdt-worker
```

After both containers are healthy, expose the loopback API only to the tailnet:

```sh
sudo tailscale serve --bg http://127.0.0.1:8080
tailscale serve status
```

The owner uses `https://optiplex.tailccb32f.ts.net`. A user who receives a
Tailscale machine share must also use this full domain name. Tailscale Serve
adds `Tailscale-User-Login`; all data endpoints reject users not listed in
`DMTARGETBOT_ALLOWED_USERS`.

Available endpoints:

- `GET /api/v1/health` (no identity required; contains no private data)
- `GET /api/v1/targets`
- `GET /api/v1/opportunities`
- legacy aliases: `/api/v1/targets/current` and
  `/api/v1/opportunities/current`

## Stop

```sh
docker compose down
```

`docker compose down` preserves the SQLite volume. Do not add `--volumes`
unless the stored catalog, snapshots, alerts, and collector offset are no longer
needed.

For backups, use SQLite's online backup mechanism rather than copying an active
database file directly.

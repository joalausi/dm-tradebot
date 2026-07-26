# OptiPlex SteamDT deployment

This Compose project runs one sequential SteamDT worker. It refreshes the base
catalog and filtered universe, then collects one configured universe chunk per
interval. Sequential execution avoids concurrent SQLite writers.

The service publishes no host ports. Its only network requirement is outbound
HTTPS access to SteamDT.

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

## Start

```sh
docker compose up --detach --build
docker compose ps
docker compose logs --follow --tail=100 steamdt-worker
```

## Stop

```sh
docker compose down
```

`docker compose down` preserves the SQLite volume. Do not add `--volumes`
unless the stored catalog, snapshots, alerts, and collector offset are no longer
needed.

For backups, use SQLite's online backup mechanism rather than copying an active
database file directly.

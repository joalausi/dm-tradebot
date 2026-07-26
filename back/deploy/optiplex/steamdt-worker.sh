#!/bin/sh
set -eu

config_path="${STEAMDT_CONFIG:-/etc/targetbot/config.yaml}"
collect_interval="${COLLECT_INTERVAL_SECONDS:-600}"
catalog_interval="${CATALOG_REFRESH_SECONDS:-86400}"
health_file="${STEAMDT_HEALTH_FILE:-/var/lib/targetbot/last-success}"
stopping=0

log() {
    printf '%s %s\n' "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" "$*"
}

require_positive_integer() {
    name="$1"
    value="$2"

    case "$value" in
        ''|*[!0-9]*)
            log "$name must be a positive integer, got: $value"
            exit 2
            ;;
    esac

    if [ "$value" -le 0 ]; then
        log "$name must be greater than zero, got: $value"
        exit 2
    fi
}

request_stop() {
    stopping=1
}

sleep_until_next_run() {
    sleep "$collect_interval" &
    sleep_pid=$!
    wait "$sleep_pid" || true
}

refresh_catalog() {
    log "checking SteamDT base catalog"
    steamdt-crawler -config "$config_path" -sync-base

    log "rebuilding filtered market universe"
    steamdt-crawler -config "$config_path" -rebuild-universe
}

collect_chunk() {
    log "collecting the next SteamDT universe chunk"
    steamdt-crawler -config "$config_path" -collect-catalog-chunk
    touch "$health_file"
}

trap request_stop INT TERM

require_positive_integer COLLECT_INTERVAL_SECONDS "$collect_interval"
require_positive_integer CATALOG_REFRESH_SECONDS "$catalog_interval"

if [ ! -r "$config_path" ]; then
    log "SteamDT config is not readable: $config_path"
    exit 2
fi

refresh_catalog
next_catalog_refresh=$(( $(date +%s) + catalog_interval ))

while [ "$stopping" -eq 0 ]; do
    collect_chunk

    now=$(date +%s)
    if [ "$now" -ge "$next_catalog_refresh" ]; then
        refresh_catalog
        next_catalog_refresh=$(( now + catalog_interval ))
    fi

    if [ "$stopping" -eq 0 ]; then
        sleep_until_next_run
    fi
done

log "SteamDT worker stopped"

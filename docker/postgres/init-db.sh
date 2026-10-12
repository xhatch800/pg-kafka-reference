#!/bin/bash
# Runs once, on the first start of an empty data volume (via /docker-entrypoint-initdb.d).
# Creates the application's database and its owner as a regular (non-superuser) role.
# The POSTGRES_USER superuser is used only here, never by the app.
set -euo pipefail

: "${APP_DB:?APP_DB must be set}"
: "${APP_USER:?APP_USER must be set}"
: "${APP_PASSWORD:?APP_PASSWORD must be set}"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname postgres \
     --set app_db="$APP_DB" \
     --set app_user="$APP_USER" \
     --set app_password="$APP_PASSWORD" <<-'EOSQL'
	CREATE ROLE :"app_user" WITH LOGIN PASSWORD :'app_password';
	CREATE DATABASE :"app_db" OWNER :"app_user";
	REVOKE ALL ON DATABASE :"app_db" FROM PUBLIC;
EOSQL

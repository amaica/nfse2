#!/usr/bin/env bash
# Atalho: sobe em primeiro plano (logs no terminal via tail)
ROOT="$(cd "$(dirname "$0")" && pwd)"
exec "$ROOT/subir-portal.sh" --fg "$@"

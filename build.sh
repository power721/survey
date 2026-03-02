#!/bin/bash
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
FRONTEND="$ROOT/frontend"
STATIC="$ROOT/backend/src/main/resources/static"

echo "Building frontend..."
cd "$FRONTEND"
npm run build

echo "Copying dist to backend static resources..."
rm -rf "$STATIC"
cp -r "$FRONTEND/dist" "$STATIC"

echo "Done. Static files copied to $STATIC"
cd "$ROOT"

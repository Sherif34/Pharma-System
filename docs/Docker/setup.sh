#!/usr/bin/env bash
# Pharma PM1 Part B starter: creates 10 placeholder services, Dockerfiles,
# docker-compose.yml and the GitHub Actions workflow.
# Run it from inside your cloned repo folder:  bash setup.sh
set -e

SERVICES="catalogue-service inventory-service receiving-expiry-service pricing-service purchasing-service stock-review-service customer-register-service operations-reporting-service access-control-service data-continuity-service"

echo "services:" > docker-compose.yml
port=3001

for s in $SERVICES; do
  mkdir -p "$s"

  cat > "$s/package.json" <<EOF
{
  "name": "$s",
  "version": "1.0.0",
  "main": "index.js",
  "scripts": { "start": "node index.js" },
  "dependencies": { "express": "^4.19.2" }
}
EOF

  cat > "$s/index.js" <<'EOF'
const express = require('express');
const app = express();
app.get('/health', (req, res) =>
  res.json({ status: 'ok', service: process.env.SERVICE_NAME }));
// TODO: replace this placeholder with your real service code
app.listen(process.env.PORT || 3000, () =>
  console.log(process.env.SERVICE_NAME + ' listening'));
EOF

  cat > "$s/Dockerfile" <<'EOF'
# ---------- Stage 1: build (install dependencies) ----------
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev
COPY . .

# ---------- Stage 2: runtime (small final image) ----------
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app /app
USER node
EXPOSE 3000
CMD ["node", "index.js"]
EOF

  printf "node_modules\n.git\n.env\n" > "$s/.dockerignore"

  cat >> docker-compose.yml <<EOF
  $s:
    build: ./$s
    environment:
      PORT: "3000"
      SERVICE_NAME: $s
    ports:
      - "$port:3000"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:3000/health || exit 1"]
      interval: 10s
      timeout: 3s
      retries: 5

EOF
  port=$((port+1))
done

mkdir -p .github/workflows
cat > .github/workflows/ci.yml <<'EOF'
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    strategy:
      fail-fast: false
      matrix:
        service:
EOF
for s in $SERVICES; do echo "          - $s" >> .github/workflows/ci.yml; done
cat >> .github/workflows/ci.yml <<'EOF'
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        if: github.event_name != 'pull_request'
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository_owner }}/pharma-${{ matrix.service }}
          tags: |
            type=sha
            type=raw,value=latest,enable={{is_default_branch}}
      - uses: docker/build-push-action@v6
        with:
          context: ./${{ matrix.service }}
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  compose-smoke-test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: docker compose up -d --build --wait
      - run: docker compose ps
      - run: docker compose down -v
EOF

echo "Done. Next: docker compose up --build"

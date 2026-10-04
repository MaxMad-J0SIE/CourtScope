# CourtScope — Project Plan

A basketball assistant that tracks game statistics, analyzes player effectiveness and measures skill improvement over time.

- **Audience:** anyone interested in basketball, me first (hard to track every game by hand).
- **Goals:** a learning project, a portfolio piece and a real tool.

## Stages

1. **Stage 1: NBA data.** Collect game and player data from an NBA data source, store it, serve it through an API and analyze it.
2. **Stage 2: Camera AI.** Track live games through a camera and generate stats automatically.

## Tech stack

| Area | Choice |
|---|---|
| Backend | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator, DevTools, Docker Compose support) |
| Build | Maven (via the Maven Wrapper: `./mvnw`) |
| Java | 25 LTS (`java.version` in `pom.xml`) |
| Database | PostgreSQL 18 (pinned version, same locally, in tests and on the server) |
| Migrations | Flyway |
| Tests | JUnit 5 + Testcontainers (temporary Postgres in Docker) |
| Stage 2 vision | Separate Python service (YOLO / OpenCV / tracking), not Java |

## Architecture

```
NBA data source ──> NbaDataProvider (interface) ──> Spring Boot app ──> PostgreSQL
Camera ──> Python vision service ──(events)──────────┘        (Stage 2)
```

- **Provider interface:** every NBA data source sits behind `NbaDataProvider`, so switching sources means changing one class.
- **Event-level data:** store play-by-play events, not only box-score totals. Stage 2 then becomes another event source feeding the same tables, and the analysis code works on camera data unchanged.

### Core data model

```
Team ─< Player
Season ─< Game ─< PlayerGameStats   (box score per player per game)
              └─< GameEvent         (play-by-play: type, player, clock, x/y, source)
```

`GameEvent.source` = `NBA_API` now, `CAMERA` in Stage 2.

## NBA data source (undecided)

There's no official public NBA API.

- **stats.nba.com** (unofficial, what `nba_api` wraps): richest data (play-by-play, shot locations), but undocumented, blocks non-browser requests, rate-limited and can change without warning.
- **Third-party APIs** (balldontlie, API-Sports via RapidAPI, SportsDataIO): documented and stable, need an API key, free tiers are limited (check current pricing).
- **Fallback:** start with a fake provider that returns sample data, so the rest of the app isn't blocked.

## Stage 1 steps

0. **Explore the data source:** call candidate APIs by hand (IntelliJ `.http` files / curl / Bruno), save sample responses as JSON, compare coverage, history, IDs, freshness, rate limits, terms of use and format quirks. The schema in step 2 follows from what's found.
1. **Spring Boot skeleton:** generated from start.spring.io, `compose.yaml` with Postgres, `spring-boot-docker-compose`. Remaining: check that `/actuator/health` responds.
2. **Schema + first Flyway migration:** teams, players, seasons, games.
3. **`NbaDataProvider` interface + one implementation:** teams and players first.
4. **Ingestion:** a `@Scheduled` job imports games and box scores. It must be safe to rerun without duplicates, and must retry and respect rate limits.
5. **Read API:** REST endpoints, e.g. `/players/{id}/stats`, `/games/{id}`.
6. **Analysis:** per-game averages, eFG%, TS%, rolling trends (improvement over time).
7. **Play-by-play ingestion** into `GameEvent` (prepares for Stage 2).
8. **Deploy** to the VPS.

Each step leaves the app working, and each one introduces one main Spring concept.

## Local development (Mac)

- **Docker** runs Postgres and any other services the app depends on. The Spring Boot app itself runs directly from IntelliJ (faster, easier debugging).
- `spring-boot-docker-compose` starts Postgres from `compose.yaml` automatically when the app starts.
- Docker runtime: **Docker Desktop** (installed). OrbStack or Colima would also work.
- Reset the local database: `docker compose down -v`.
- Write code on the Mac, push to GitHub, deploy to the server. Don't develop on the server.

## Hosting

**Plan: a VPS** (e.g. Hetzner, DigitalOcean, Linode, OVH) running Docker Compose.

- **Size:** 2 GB RAM minimum, 4 GB comfortable, 2 vCPU, about 40 GB disk.
- **Alternatives considered:** a PaaS (Railway, Render, Fly.io) means less ops work but less learning. The big clouds (AWS/GCP/Azure) are overkill for now.

```
VPS (Ubuntu + Docker Compose)
├── caddy          ports 80/443, automatic HTTPS → app
├── courtscope-app Spring Boot
└── postgres       internal Docker network only
```

### Server security checklist

- [ ] SSH with keys only, password login off, root login blocked
- [ ] Firewall allows only ports 22, 80 and 443
- [ ] Postgres never exposed publicly; a dedicated app database user (not `postgres`)
- [ ] Automatic security updates (`unattended-upgrades`)
- [ ] Secrets in an `.env` file on the server, never committed
- [ ] Containers use `restart: unless-stopped`
- [ ] Nightly `pg_dump` sent to off-server object storage (provider storage or Backblaze B2)
- [ ] A domain name for HTTPS

### Deployment

- At first: SSH in, then `git pull && docker compose up -d --build`.
- Later: GitHub Actions builds the image and deploys it automatically.

## Stage 2 notes

- Real-time video analysis needs an **NVIDIA GPU**, and normal VPS plans don't have one.
- Options: run the vision service on my own machine, or rent a GPU by the hour only while processing a game.
- Either way it sends events to the API on the VPS, so the architecture doesn't change.

## Current state

- Branch `1stStage`. Spring Boot 4.1.1 project generated from start.spring.io, package `com.madmax.courtscope` (main class `CourtscopeApplication`).
- Maven Wrapper (`mvnw`, `.mvn/`) included. `compose.yaml` and Testcontainers both use `postgres:18`.
- JDK 25 and Docker Desktop are installed. The IntelliJ project SDK still needs to be switched from 26 to 25.
- Not yet verified: `./mvnw test` and starting the app.
- `.gitignore` covers `target/`, `.idea/`, `*.iml`, `out/`, `.DS_Store`.

## Open decisions

- [ ] NBA data source (stats.nba.com vs a third-party API)
- [ ] VPS provider
- [x] Docker runtime on the Mac: Docker Desktop
- [ ] UI: separate frontend (React + Vite, recommended later) vs Thymeleaf; decide once real data is flowing
- [ ] Domain name

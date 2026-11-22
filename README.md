# Penalty Bot

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Invite Bot](https://img.shields.io/badge/Discord-Invite%20Bot-5865F2?logo=discord&logoColor=white)](https://penaltybot.esemudeo.com/invite)

A Discord bot for tracking penalties within guild communities. Members can report penalties, view individual or
aggregated summaries, and guild admins can configure penalty types, permissions, and payment links through a web-based
admin panel.

**Want to use the bot?** Click the "Invite Bot" badge above
or https://penaltybot.esemudeo.com/invite to add a running instance to your Discord server — no setup
required. Alternatively, use
the [direct Discord invite](https://discord.com/oauth2/authorize?client_id=1481017571552661524) if the redirect is
unavailable.

If you prefer to host your own instance, follow the instructions below.

## Features

- **Slash Commands** — Report penalties, view individual penalty history, generate monthly summaries
- **Context Menu Integration** — Key commands accessible via right-click on a user
- **Web Admin Panel** — Vaadin-based settings UI protected by Discord OAuth2
- **Configurable Penalty Types** — Define custom penalty categories with optional pricing per guild
- **Granular Permissions** — Per-command role-based access control (minimum role + explicit roles)
- **Multi-Guild Support** — Each guild has isolated settings, penalty types, and permissions
- **Notification Channel** — Optionally broadcast penalty reports to a designated channel
- **PayPal Integration** — Link a PayPal.me account for easy payment collection in summaries

## Tech Stack

| Technology                                  | Purpose               |
|---------------------------------------------|-----------------------|
| [Quarkus 3](https://quarkus.io/)            | Application framework |
| Java 21                                     | Language              |
| [JDA 6](https://github.com/discord-jda/JDA) | Discord API wrapper   |
| [Vaadin 24](https://vaadin.com/)            | Web admin UI          |
| PostgreSQL                                  | Database              |
| [Flyway](https://flywaydb.org/)             | Database migrations   |
| Hibernate + Panache                         | ORM                   |
| Lombok                                      | Boilerplate reduction |

## Prerequisites

- **JDK 21** (e.g. via [SDKMAN](https://sdkman.io/))
- **Maven 3.9+** (or use the included `./mvnw` wrapper)
- **A Discord Application** with bot token and OAuth2 credentials — see [Getting Started](#getting-started)
- **PostgreSQL** (only for production — dev mode auto-provisions a container via Quarkus Dev Services)

## Getting Started

### 1. Create a Discord Application

1. Go to the [Discord Developer Portal](https://discord.com/developers/applications) and create a new application.
2. Under **Bot**, copy the bot token.
3. Under **OAuth2**, note the client ID and client secret.
4. Add a redirect URI (e.g. `http://localhost:8080/oauth/callback` for local development).
5. Invite the bot to your server with the `bot` and `applications.commands` scopes.

### 2. Configure Environment Variables

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

> In dev mode, `DB_JDBC_URL` and `DB_PASSWORD` are optional — Quarkus Dev Services spins up a PostgreSQL container
> automatically.

### 3. Run in Dev Mode

```bash
./mvnw quarkus:dev
```

The bot connects to Discord immediately. The Quarkus Dev UI is available at `http://localhost:8080/q/dev/`.

## Configuration

| Variable                | Required  | Description                                                           |
|-------------------------|-----------|-----------------------------------------------------------------------|
| `DISCORD_BOT_TOKEN`     | Yes       | JDA bot token from the Developer Portal                               |
| `DISCORD_CLIENT_ID`     | Yes       | OAuth2 client ID                                                      |
| `DISCORD_CLIENT_SECRET` | Yes       | OAuth2 client secret                                                  |
| `DISCORD_REDIRECT_URI`  | Yes       | OAuth2 callback URL (must match the ones in Discord Developer Portal) |
| `APP_BASE_URL`          | Yes       | Base URL of the web UI (e.g. `https://penalty.example.com`)           |
| `DB_JDBC_URL`           | Prod only | PostgreSQL JDBC URL                                                   |
| `DB_PASSWORD`           | Prod only | PostgreSQL password                                                   |

## Bot Commands

| Command            | Description                                                                                  |
|--------------------|----------------------------------------------------------------------------------------------|
| `/penalty`         | Report a penalty for a guild member (also available via user context menu). Negative amounts are corrections: they need a comment and the `penalty-remove` role from the setup panel (default: owner/admins). Nothing is ever deleted. |
| `/penalty-show`    | View penalties for a specific member in a given month (also available via user context menu) |
| `/penalty-summary` | View aggregated penalty summary for all members in a given month                             |
| `/penalty-setup`   | Generate a secure, time-limited link to the web admin panel                                  |

## Web Admin Panel

The admin panel is accessible via the link generated by `/penalty-setup`. It provides three configuration sections:

- **Command Permissions** — Set a minimum role or explicit roles for each bot command
- **Penalty Types** — Create, edit, activate/deactivate, and delete penalty categories with optional pricing
- **Common Settings** — Configure a PayPal.me username and a notification channel for penalty reports

Authentication uses Discord OAuth2 — only guild members with sufficient permissions can access the panel.

## Building for Production

```bash
# Build
./mvnw package -Pproduction

# Run standalone
java -jar target/quarkus-app/quarkus-run.jar
```

The `-Pproduction` profile triggers the Vaadin frontend build (`prepare-frontend` + `build-frontend`) and excludes the
Vaadin dev server. This is required for any deployment outside of dev mode.

### Docker Deployment

Two Docker Compose files are included. Rename the one you need to `docker-compose.yml`:

| File                                                   | Purpose                                                                                         |
|--------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| [`docker-compose.build.yml`](docker-compose.build.yml) | Builds the Docker image locally from source (`build:`)                                          |
| [`docker-compose.prod.yml`](docker-compose.prod.yml)   | Uses a pre-built image (`image: penalty-bot:latest`) and includes a bundled PostgreSQL instance |

**Option A — Build locally and run:**

```bash
cp docker-compose.build.yml docker-compose.yml
./mvnw package -Pproduction -DskipTests
docker compose up -d --build
```

**Option B — Deploy a pre-built image (e.g. on a server):**

```bash
# On your build machine:
./mvnw package -Pproduction -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t penalty-bot:1.0.0 .
docker save penalty-bot:1.0.0 | gzip > penalty-bot-1.0.0.tar.gz
scp penalty-bot-1.0.0.tar.gz your-server:~

# On the server:
docker load < penalty-bot-1.0.0.tar.gz
cp docker-compose.prod.yml docker-compose.yml
# Create .env with your configuration (see above)
docker compose up -d
```

> If you already have a database, remove the `db` service from the compose file and set `DB_JDBC_URL` and `DB_PASSWORD`
> in your `.env`.

## Project Structure

```
com.esemudeo.quarkus.penaltybot/
├── configuration/            # Guild administration via web UI
│   ├── auth/                 # Discord OAuth2 login and session management
│   ├── command/              # Slash command that generates admin panel links
│   ├── commandpermission/    # Who may use which bot command
│   ├── global/               # Guild-wide settings (PayPal, notification channel)
│   └── penaltytype/          # Custom penalty categories and pricing
├── penalty/                  # Core domain: recording and querying penalties
│   ├── command/              # User-facing commands for penalties
│   ├── listener/             # Discord modal form processing
│   ├── model/                # Penalty domain model
│   └── repository/           # Penalty data access
├── permission/               # Role-based access control enforcement, could aswell count as configuration
└── shared/                   # Cross-cutting bot infrastructure
    ├── command/              # Shared command abstractions
    ├── exception/            # Common error handling
    ├── init/                 # Bot startup and guild initialization
    └── listener/             # Discord event routing
```

## Roadmap

- Roles for receiving penalty reports
- Support for negative penalties (credits or corrections)
- Auto-delete notification messages after a configurable time
- Permission checks before sending to notification channel (graceful error handling)
- extend test environment
- improve texts
- find better solution for order of command permissions

## License

This project is licensed under the [Apache License 2.0](LICENSE).

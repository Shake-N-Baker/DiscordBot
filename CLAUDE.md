# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack

Spring Boot 3.4.1 + JDA 5.2.2 Discord bot, Java 17, Gradle, MongoDB for persistence. Root project name is `ianbot` (`settings.gradle`); the Spring `@SpringBootApplication` entry point is `IanBotApplication`.

## Commands

```
./gradlew bootRun        # run the bot (requires MongoDB + DISCORD_BOT_TOKEN)
./gradlew build          # compile + test
./gradlew test           # run tests (JUnit Platform — no tests exist yet)
./gradlew test --tests "FQCN.method"   # single test once tests exist
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Prerequisites to run locally

1. MongoDB Community Server running on `localhost:27017` (database `ianbot` — auto-created). Windows: `net start MongoDB`. See README for Mac.
2. `DISCORD_BOT_TOKEN` provided either as an env var or in a `.env` file at the project root (copy `.env.template`). Resolution order is in `JdaConfig.resolveDiscordToken` — env var wins, then `.env`, otherwise startup fails.

## Architecture

The bot is a single Spring context that wires JDA as a managed `@Bean`:

- `JdaConfig#jda` builds the `JDA` instance, registers the two listeners, and `awaitReady()`s before the bean is returned. Spring shuts it down on context close via `destroyMethod = "shutdown"`.
- `JdaConfig#registerSlashCommands` runs on `ApplicationReadyEvent` and pushes the slash command set to each configured guild (`discord.guilds.test-id`, `discord.guilds.fuzzy-id` in `application.properties`). Commands are registered per-guild, not globally — adding a new guild means adding both a property in `DiscordProperties.Guilds` and a `registerForGuild` call.

Dispatch is registry-based, not a switch statement:

- **Slash commands** implement `command.SlashCommand` (`getCommandData()` + `execute(event)`). `BotCommands` injects `List<SlashCommand>` from Spring, indexes them by `getCommandData().getName()`, and dispatches by `event.getName()`. `JdaConfig.registerSlashCommands` collects `SlashCommandData` from the same list — definition and behavior live together, and adding a command requires no edits to `BotCommands` or `JdaConfig`.
- **Buttons** implement `command.ButtonHandler` (`getPrefix()` + `handle(event, payload)`). Button IDs are formatted `prefix:payload` (delimiter is `ButtonHandler.DELIMITER`). `ButtonInteractions` splits on the first delimiter, looks up the handler by prefix, and hands the rest to the handler as opaque payload — so the handler can encode per-interaction state (action, game id, bet, etc.) without a new constant or dispatcher branch. A `SlashCommand` bean is free to also implement `ButtonHandler` (see `Blackjack`) so one class owns both sides of a feature.

Persistence is a thin Spring Data MongoDB layer for **shared, cross-feature** user state only:

- `IBUser` (`@Document(collection = "user")`) holds `discordId`, `points`, `lastFreeClaimTime`. `discordId` has a unique index; the Mongo `_id` is a separate generated string.
- `IBUserRepository extends MongoRepository<IBUser, String>` with one derived query `findByDiscordId`.
- `UserService.getOrCreate(discordId)` returns an in-memory `IBUser` for new users; commands must call `userService.save(...)` to persist any change.

**Per-game state does not belong in `IBUser`.** Each game owns its own `@Document` + `MongoRepository` (keyed by `discordId` if scoped per user) alongside its command class — keeps `IBUser` lean and lets one game evolve its schema without touching anything else.

## Adding a new slash command

1. Create a `@Service` in `command/` implementing `SlashCommand`. Return a `Commands.slash(name, description)` (plus any `OptionData`) from `getCommandData()`, and put the behavior in `execute(event)`. Inject `UserService` and call `getOrCreate(...)` only if the command needs the user record.
2. That's it — Spring picks it up and both the dispatcher and the registration call see it on next startup.

Slash commands only appear in a guild after `registerSlashCommands` runs on next startup against that guild ID.

## Adding a new button

Implement `ButtonHandler` on the command class (or a dedicated bean) and pick a `getPrefix()` unique across the bot. Emit buttons with IDs `prefix:<your-payload>` — payload can carry action, game id, etc., and is parsed inside `handle`. No edits to `ButtonInteractions` needed.

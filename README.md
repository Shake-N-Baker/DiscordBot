# IanBot - Spring Boot Version

This is a Discord bot built with JDA and MongoDB, converted to use Spring Boot for dependency injection and configuration management.

## Features

- Spring Boot application with proper component scanning
- Dependency injection for database connections and event handlers  
- Configuration via `application.properties`
- MongoDB integration using Spring Data MongoDB
- Event listeners as Spring components
- Command handling through properly injected services

## Setup

1. Make sure you have Java 17+ installed
2. Ensure MongoDB is running locally on port 27017
3. Set your Discord bot token in `application.properties` or via environment variable:
   ```
   DISCORD_TOKEN=your_bot_token_here
   ```

## Running the Application

```bash
./gradlew bootRun
```

Or build and run the jar:

```bash
./gradlew build
java -jar build/libs/ianbot-1.0-SNAPSHOT.jar
```

## Project Structure

- `IanBotApplication.java` - Main Spring Boot application class
- `IanBot.java` - Bot initialization with JDA setup 
- `BotCommands.java` - Slash command event listener (Spring component)
- `ButtonInteractions.java` - Button interaction event listener (Spring component)
- `config/MongoConfig.java` - MongoDB configuration
- `command/` - Command handler services (Spring components)
- `model/` - Data models

## Dependencies

- Spring Boot 3.2.0
- JDA 5.2.2 
- Spring Data MongoDB

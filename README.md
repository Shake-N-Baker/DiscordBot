# DiscordBot
This repository contains code for running a bot in Discord.

## Setup
This repo uses MongoDB to store data and persist it across chat sessions and runs on Java.

### MongoDB
Install [MongoDB Community Server](https://www.mongodb.com/try/download/community)

Can also install [MongoDB Compass](https://www.mongodb.com/try/download/compass) as a GUI to visualize/query/manage data.

Windows:

Start / stop MongoDB service

<pre>
net start MongoDB
net stop MongoDB
</pre>

Mac:

<pre>
% which mongod
/Users/ianbaker/Development/tools/mongodb-macos-x86_64-8.0.4/bin/mongod
</pre>

Start

<pre>
mongod --dbpath=/Users/ianbaker/Development/data/db
</pre>

### Java 17
Install Java 17

### Secrets
Set the environment variable DISCORD_BOT_TOKEN

Windows:

<pre>
$env:DISCORD_BOT_TOKEN = ""
</pre>

Mac:

<pre>
export DISCORD_BOT_TOKEN=""
</pre>

Or create a .env file based on the .env.template and fill in the secrets.

## Run

<pre>
./gradlew bootRun
</pre>

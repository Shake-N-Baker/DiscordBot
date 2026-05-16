package com.ianscottbaker.ianbot.repository;

import com.ianscottbaker.ianbot.command.spire.SpireRun;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpireRunRepository extends MongoRepository<SpireRun, String> {
    Optional<SpireRun> findByDiscordId(String discordId);
    void deleteByDiscordId(String discordId);
}

package com.ianscottbaker.ianbot.repository;

import com.ianscottbaker.ianbot.command.blackjack.BlackjackGame;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BlackjackGameRepository extends MongoRepository<BlackjackGame, String> {
    Optional<BlackjackGame> findByDiscordId(String discordId);
    void deleteByDiscordId(String discordId);
}

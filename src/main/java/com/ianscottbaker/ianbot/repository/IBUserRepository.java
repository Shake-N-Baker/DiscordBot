package com.ianscottbaker.ianbot.repository;

import com.ianscottbaker.ianbot.model.IBUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IBUserRepository extends MongoRepository<IBUser, String> {
    Optional<IBUser> findByDiscordId(String discordId);
}

package com.ianscottbaker.ianbot.service;

import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.repository.IBUserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final IBUserRepository repository;

    public UserService(IBUserRepository repository) {
        this.repository = repository;
    }

    public IBUser getOrCreate(String discordId) {
        return findExisting(discordId).orElseGet(() -> new IBUser(discordId));
    }

    public Optional<IBUser> findExisting(String discordId) {
        return repository.findByDiscordId(discordId);
    }

    public IBUser save(IBUser user) {
        return repository.save(user);
    }
}

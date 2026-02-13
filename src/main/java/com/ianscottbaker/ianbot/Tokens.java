package com.ianscottbaker.ianbot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Tokens {
    public static final String IAN_BOT_TOKEN;

    static {
        // First priority is pulling token from environment variable
        String token = System.getenv("IAN_BOT_TOKEN");

        if (token == null || token.isBlank()) {
            // Second priority is pulling token from token.properties
            Path p = Path.of("token.properties");
            if (Files.exists(p)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(p)) {
                    props.load(in);
                    token = props.getProperty("token.ianbot");
                } catch (IOException ignored) {
                }
            }
        }

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Bot token not configured. Set IAN_BOT_TOKEN or create token.properties from token.properties.template.");
        }

        IAN_BOT_TOKEN = token;
    }
}

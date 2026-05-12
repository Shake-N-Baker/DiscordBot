package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.config.DiscordProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DiscordProperties.class)
public class IanBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(IanBotApplication.class, args);
    }
}

package com.ianscottbaker.ianbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord")
public class DiscordProperties {
    private Guilds guilds = new Guilds();

    public Guilds getGuilds() {
        return guilds;
    }
    public void setGuilds(Guilds guilds) {
        this.guilds = guilds;
    }

    public static class Guilds {
        private String testId;
        private String fuzzyId;

        public String getTestId() {
            return testId;
        }
        public void setTestId(String testId) {
            this.testId = testId;
        }
        public String getFuzzyId() {
            return fuzzyId;
        }
        public void setFuzzyId(String fuzzyId) {
            this.fuzzyId = fuzzyId;
        }
    }
}

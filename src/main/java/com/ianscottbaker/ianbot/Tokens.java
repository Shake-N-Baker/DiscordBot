package com.ianscottbaker.ianbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Tokens {
    // Do not upload below token
    @Value("${discord.token}")
    public static String ianBotToken;
}

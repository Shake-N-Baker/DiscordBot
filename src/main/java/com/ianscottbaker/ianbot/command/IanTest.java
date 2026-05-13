package com.ianscottbaker.ianbot.command;

import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.service.UserService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IanTest implements SlashCommand {
    public static final String NAME = "iantest";

    private final UserService userService;

    public IanTest(UserService userService) {
        this.userService = userService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash(NAME, "debug command");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userName = event.getUser().getGlobalName();
        String userId = event.getUser().getId();
        Optional<IBUser> existing = userService.findExisting(userId);
        IBUser ibUser = existing.orElseGet(() -> new IBUser(userId));
        String suffix = existing.isPresent() ? "existing" : "new";
        event.reply(String.format(
                "name: %s, id: %s, points: %d, lastFreeClaimTime: %d (%s user)",
                userName, userId, ibUser.getPoints(), ibUser.getLastFreeClaimTime(), suffix
        )).setEphemeral(true).queue();
    }
}

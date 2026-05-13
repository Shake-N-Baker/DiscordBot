package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.command.SlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BotCommands extends ListenerAdapter {
    private final Map<String, SlashCommand> commandsByName;

    public BotCommands(List<SlashCommand> commands) {
        this.commandsByName = commands.stream()
                .collect(Collectors.toMap(c -> c.getCommandData().getName(), Function.identity()));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        SlashCommand command = commandsByName.get(event.getName());
        if (command == null) {
            event.reply(String.format("Unknown command: %s", event.getName())).setEphemeral(true).queue();
            return;
        }
        command.execute(event);
    }
}

package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import reactor.core.publisher.Mono;

public class Skip implements Command {
    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Skip/remove first song from queue";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        int position = Math.toIntExact(event.getOption("position")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(0L));
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);

        if (position == 0){
            if (GuildAudioManager.of(guildId).getScheduler().skip())
                return event.reply().withContent("Skipping to next song!");
            return event.reply().withContent("Queue's currently empty!");
        }

        if (GuildAudioManager.of(guildId).getScheduler().skip(position - 1))
            return event.reply().withContent(String.format("Skipping song at position `%d` in queue!", position));
        return event.reply().withContent("Queue's currently empty!");

    }
}

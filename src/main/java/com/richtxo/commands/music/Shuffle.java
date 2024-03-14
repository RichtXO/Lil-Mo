package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.audio.TrackScheduler;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class Shuffle implements Command {
    @Override
    public String getName() {
        return "shuffle";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Shuffles the queue";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = Objects.requireNonNull(event.getInteraction().getMember().orElse(null)).getGuildId();
        TrackScheduler scheduler = GuildAudioManager.of(guildId).getScheduler();
        String user = Objects.requireNonNull(event.getInteraction().getMember().orElse(null))
                .getNicknameMention();

        scheduler.shuffle();
        return event.reply().withContent(String.format("%s has shuffled the queue!", user));
    }
}

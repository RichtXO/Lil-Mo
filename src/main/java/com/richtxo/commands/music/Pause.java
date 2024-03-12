package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class Pause implements Command {
    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Pauses music";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = Objects.requireNonNull(event.getInteraction().getMember().orElse(null)).getGuildId();
        AudioPlayer player = GuildAudioManager.of(guildId).getPlayer();
        String user = Objects.requireNonNull(event.getInteraction().getMember().orElse(null))
                .getNicknameMention();

        if (!player.isPaused())
            return event.reply().withContent(String.format("Already paused, %s!", user));

        player.setPaused(true);
        return event.reply().withContent(String.format("%s paused music!", user));
    }
}

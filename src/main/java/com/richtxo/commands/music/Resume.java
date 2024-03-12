package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class Resume implements Command {
    @Override
    public String getName() {
        return "resume";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Resumes music";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = Objects.requireNonNull(event.getInteraction().getMember().orElse(null)).getGuildId();
        AudioPlayer player = GuildAudioManager.of(guildId).getPlayer();
        String user = Objects.requireNonNull(event.getInteraction().getMember().orElse(null))
                .getNicknameMention();

        if (!player.isPaused())
            return event.reply().withContent(String.format("Already playing music, %s!", user));

        player.setPaused(false);
        return event.reply().withContent(String.format("%s resumed music!", user));
    }
}

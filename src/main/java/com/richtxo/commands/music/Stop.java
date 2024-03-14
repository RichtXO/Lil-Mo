package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class Stop implements Command {
    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Stop playing and clear queue";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getMember().get().getGuildId();
        VoiceState userVS = event.getInteraction().getMember().get().getVoiceState().block();
        String user = Objects.requireNonNull(event.getInteraction().getMember().orElse(null))
                .getNicknameMention();

        if (userVS == null)
            return event.reply().withContent(String.format("%s is not in any voice channels", user));

        VoiceConnection vc = event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId).block();
        if (vc != null && !Objects.equals(vc.getChannelId().block(), userVS.getChannelId().get())){
            String bot = Objects.requireNonNull(event.getClient().getSelf().block()).getUsername();
            return event.reply().withContent(String.format("%s is not in the same voice channel as %s", user, bot));
        }

        GuildAudioManager.of(guildId).getPlayer().stopTrack();
        GuildAudioManager.of(guildId).getScheduler().clear();

        return event.reply().withContent("Stopping music and clearing queue!");
    }
}

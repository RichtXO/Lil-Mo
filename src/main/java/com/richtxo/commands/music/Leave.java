package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

public class Leave implements Command {
    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Makes Lil Mo leave current voice channel";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        String user = Objects.requireNonNull(event.getInteraction().getMember().orElse(null)).getNicknameMention();

        // Checking if Lil Mo currently not in any voice channels
        Snowflake guildId = event.getInteraction().getMember().get().getGuildId();
        VoiceConnection vc = event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId).block();
        if (vc == null)
            return event.reply("Currently not in any voice channels!").then();

        return event.deferReply()
                .then(Mono.justOrEmpty(event.getInteraction().getMember()))
                .flatMap(Member::getVoiceState)
                .flatMap(vs -> event.getClient().getVoiceConnectionRegistry().getVoiceConnection(vs.getGuildId()))
                .flatMap(voiceConnection -> {
                    Mono<Snowflake> channelId = Objects.requireNonNull(voiceConnection.getChannelId());
                    return voiceConnection.disconnect()
                            .then(event.editReply(
                                    String.format("Leaving `\uD83d\uDD0A %s`!",
                                            Objects.requireNonNull(Objects.requireNonNull(event.getClient()
                                                            .getChannelById(channelId.block()).block()).getRestChannel()
                                                    .getData().block()).name().get())));
                })
                // Detecting if user is not in voice channel
                .switchIfEmpty(event.editReply(
                        String.format("%s must join a voice channel first!", user)))
                .onErrorResume(t -> {
                    return event.editReply("Something happened...");
                })
                .then();
    }
}

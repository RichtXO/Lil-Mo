package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
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
        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceChannel.isMemberConnected(event.getClient().getSelfId()))
                .defaultIfEmpty(false)
                .flatMap(isConnected -> {
                    if (isConnected){
                        Snowflake guildId = event.getInteraction().getGuildId().orElse(Snowflake.of(0));
                        GuildAudioManager.of(guildId).getPlayer().stopTrack();
                        GuildAudioManager.of(guildId).getScheduler().clear();
                        return event.reply("Stopping music and clearing queue!");
                    }

                    return event.reply(
                            String.format("Not in the same voice channel as %s!", member.getNicknameMention()));
                });
    }
}

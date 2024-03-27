package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
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
        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceChannel.isMemberConnected(event.getClient().getSelfId()))
                .defaultIfEmpty(false)
                .flatMap(isConnected -> {
                    if (isConnected){
                        Snowflake guildId = event.getInteraction().getGuildId().orElse(Snowflake.of(0));
                        AudioPlayer player = GuildAudioManager.of(guildId).getPlayer();

                        if (!player.isPaused())
                            return event.reply(String.format("Already playing music, %s!", member.getNicknameMention()));

                        player.setPaused(false);
                        return event.reply(String.format("%s resumed music!", member.getNicknameMention()));
                    }

                    return event.reply(
                            String.format("Not in the same voice channel as %s!", member.getNicknameMention()));
                });
    }
}

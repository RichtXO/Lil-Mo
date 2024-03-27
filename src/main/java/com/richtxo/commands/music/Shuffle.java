package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.audio.TrackScheduler;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

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
        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceChannel.isMemberConnected(event.getClient().getSelfId()))
                .defaultIfEmpty(false)
                .flatMap(isConnected -> {
                    if (isConnected){
                        Snowflake guildId = event.getInteraction().getGuildId().orElse(Snowflake.of(0));
                        TrackScheduler scheduler = GuildAudioManager.of(guildId).getScheduler();
                        scheduler.shuffle();
                        return event.reply(String.format("%s has shuffled the queue!", member.getNicknameMention()));
                    }

                    return event.reply(
                            String.format("Not in the same voice channel as %s!", member.getNicknameMention()));
                });
    }
}

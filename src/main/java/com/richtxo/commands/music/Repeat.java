package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.audio.TrackScheduler;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

public class Repeat implements Command {
    @Override
    public String getName() {
        return "repeat";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Repeats current song";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceChannel.isMemberConnected(event.getClient().getSelfId()))
                .defaultIfEmpty(false)
                .flatMap(isConnected -> {
                    if (isConnected) {
                        Snowflake guildId = event.getInteraction().getGuildId().orElse(Snowflake.of(0));
                        TrackScheduler scheduler = GuildAudioManager.of(guildId).getScheduler();
                        if (scheduler.isRepeating())
                            return event.reply("Already repeating, %s".formatted(member.getNicknameMention()));

                        scheduler.setRepeating(true);
                        return event.reply("%s is repeating current song!".formatted(member.getNicknameMention()));
                    }

                    return event.reply("Not in the same voice channel as %s!".formatted(member.getNicknameMention()));
                });
    }
}

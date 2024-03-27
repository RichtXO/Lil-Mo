package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
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
        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceChannel.isMemberConnected(event.getClient().getSelfId()))
                .defaultIfEmpty(false)
                .flatMap(isConnected -> {
                    if (isConnected){
                        Snowflake guildId = event.getInteraction().getGuildId().orElse(Snowflake.of(0));
                        int position = Math.toIntExact(event.getOption("position")
                                .flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(0L));

                        if (position == 0){
                            if (GuildAudioManager.of(guildId).getPlayer().getPlayingTrack() == null)
                                return event.reply("Currently not playing anything!");
                            if (GuildAudioManager.of(guildId).getScheduler().skip())
                                return event.reply("Skipping to next song!");
                            GuildAudioManager.of((guildId)).getPlayer().stopTrack();
                            return event.reply("Skipping current song!");
                        }

                        GuildAudioManager.of(guildId).getScheduler().skip(position - 1);
                        return event.reply(String.format("Skipping song at position `%d` in queue!", position));
                    }

                    return event.reply(
                            String.format("Not in the same voice channel as %s!", member.getNicknameMention()));
                });
    }
}

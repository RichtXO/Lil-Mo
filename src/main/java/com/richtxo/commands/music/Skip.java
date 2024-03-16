package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.util.Objects;

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
        int position = Math.toIntExact(event.getOption("position")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong).orElse(0L));
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
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

        if (position == 0){
            if (GuildAudioManager.of(guildId).getScheduler().skip())
                return event.reply().withContent("Skipping to next song!");

            if (GuildAudioManager.of(guildId).getPlayer().getPlayingTrack() == null)
                return event.reply().withContent("Currently not playing anything!");

            GuildAudioManager.of((guildId)).getPlayer().stopTrack();
            return event.reply().withContent("Skipping current song!");
        }

        if (GuildAudioManager.of(guildId).getScheduler().skip(position - 1))
            return event.reply().withContent(String.format("Skipping song at position `%d` in queue!", position));

        return event.reply().withContent("Queue's not that long!");
    }
}

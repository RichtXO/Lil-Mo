package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import static com.richtxo.LilMo.PLAYER_MANAGER;

public class Play implements Command {
    @Override
    public String getName() {
        return "play";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Play track or playlist with a link or search query";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String searchQuery = event.getOption("link-or-query")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("");

        Boolean ifPriority = event.getOption("priority")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean).orElse(false);


        // Joining into voice channel if not already in one
        Snowflake guildId = event.getInteraction().getMember().get().getGuildId();
        VoiceState userVS = event.getInteraction().getMember().get().getVoiceState().block();
        String user = Objects.requireNonNull(event.getInteraction().getMember().orElse(null))
                .getNicknameMention();
        VoiceConnection vc = event.getClient().getVoiceConnectionRegistry().getVoiceConnection(guildId).block();

        if (userVS == null) {
            return event.reply().withContent(String.format("%s is not in any voice channels", user));
        }

        if (vc != null && !Objects.equals(vc.getChannelId().block(), userVS.getChannelId().get())){
            String bot = Objects.requireNonNull(event.getClient().getSelf().block()).getUsername();
            return event.reply().withContent(String.format("%s is not in the same voice channel as %s", user, bot));
        }


        return event.deferReply()
                .then(Mono.justOrEmpty(event.getInteraction().getMember()))
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(channel -> {
                    AudioProvider voice = GuildAudioManager.of(guildId).getProvider();

                    if (isURL(searchQuery)){
                        return Join.autoDisconnect(channel, voice).and(loadItem(event, searchQuery));
                    } else {
                        return Join.autoDisconnect(channel, voice).then();
                    }

                })
                .onErrorResume(t -> {
                    return event.editReply("Something happened...").then();
                })
                .then();
    }

    private boolean isURL(String input){
        try {
            new URI(input);
            return true;
        } catch (URISyntaxException ignored){
            return false;
        }
    }

    private Mono<Object> loadItem(ChatInputInteractionEvent event, String url){
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        return Mono.create(monoSink -> PLAYER_MANAGER.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                event.editReply(String.format("Adding song `%s` to queue...", audioTrack.getInfo().title)).block();
                play(guildId, audioTrack);
                monoSink.success();
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                event.editReply(String.format("Adding playlist: `%s` to queue...", audioPlaylist.getName()));
                for (AudioTrack track : audioPlaylist.getTracks()) {
                    play(guildId, track);
                }
                monoSink.success();
            }

            @Override
            public void noMatches() {
                event.editReply(String.format("Can't find match to `%s`", url));
                monoSink.error(new Exception("No match!"));
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.editReply(String.format("Can't play `%s`", url));
                monoSink.error(e);
            }
        }));
    }

    private void play(Snowflake guildId, AudioTrack track){
        GuildAudioManager.of(guildId).getScheduler().play(track);
    }
}

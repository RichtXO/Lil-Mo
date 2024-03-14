package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import com.richtxo.util.spotify.SpotifyFetch;
import com.richtxo.util.spotify.SpotifyPlaylist;
import com.richtxo.util.spotify.SpotifySong;
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

        String provider = event.getOption("provider")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString).orElse("ytsearch");


        // Joining into voice channel if not already in one
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


        return event.deferReply()
                .then(Mono.justOrEmpty(event.getInteraction().getMember()))
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(channel -> {
                    AudioProvider voice = GuildAudioManager.of(guildId).getProvider();
                    if (isURL(searchQuery) && searchQuery.toUpperCase().contains("spotify".toUpperCase()))
                        return Join.autoDisconnect(channel, voice).and(loadSpotifyItem(event, searchQuery, provider));
                    return Join.autoDisconnect(channel, voice).and(loadItem(event, searchQuery, provider));
                })
                .onErrorResume(t -> {
                    return event.editReply("Something happened...").then();
                })
                .then();
    }

    private boolean isURL(String input){
        try {
            new URI(input).toURL();
            return true;
        } catch (Exception ignored){
            return false;
        }
    }

    private Mono<Void> loadSpotifyItem(ChatInputInteractionEvent event, String url, String provider){
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        SpotifyFetch spotifyFetch = new SpotifyFetch();

        if (url.toUpperCase().contains("track".toUpperCase())){
            SpotifySong song = spotifyFetch.fetchSong(url);
            return Mono.create(monoSink -> PLAYER_MANAGER.loadItem(String.format("%s: %s", provider, song.toString())
                    , new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    // Shouldn't come in here
                    monoSink.error(new Exception("Not Spotify URL!"));
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    // Play the first result
                    if (audioPlaylist.isSearchResult()){
                        event.editReply(String.format("Adding spotify song `%s` to queue...", song.getTitle())).block();
                        play(guildId, audioPlaylist.getTracks().getFirst());
                    }
                }

                @Override
                public void noMatches() {
                    event.editReply(String.format("Can't find match to `%s`", song.toString())).block();
                    monoSink.error(new Exception("No match!"));
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    event.editReply(String.format("Can't play `%s`", song.toString())).block();
                    monoSink.error(e);
                }
            }));
        }

        if (url.toUpperCase().contains("playlist".toUpperCase())){
            SpotifyPlaylist playlist = spotifyFetch.fetchPlaylist(url);
            loadSpotifySongs(event, provider, guildId, playlist);
            return event.editReply(String.format("Added spotify playlist: `%s`!", playlist.getTitle())).then();
        }

        if (url.toUpperCase().contains("album".toUpperCase())){
            SpotifyPlaylist playlist = spotifyFetch.fetchAlbum(url);
            loadSpotifySongs(event, provider, guildId, playlist);
            return event.editReply(String.format("Added spotify album: `%s`!", playlist.getTitle())).then();
        }

        return event.editReply("Couldn't find spotify link!").then();
    }

    private void loadSpotifySongs(ChatInputInteractionEvent event, String provider, Snowflake guildId, SpotifyPlaylist playlist) {
        for (SpotifySong song : playlist.getSongs()){
            PLAYER_MANAGER.loadItem(String.format("%s: %s", provider, song.toString()), new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    // Shouldn't come in here
                    return;
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    if (audioPlaylist.isSearchResult()){
                        play(guildId, audioPlaylist.getTracks().getFirst());
                    }
                }

                @Override
                public void noMatches() {
                    event.editReply(String.format("Can't find match to `%s`", song.toString())).block();
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    event.editReply(String.format("Can't play `%s`", song.toString())).block();
                }
            });
        }
    }

    private Mono<Object> loadItem(ChatInputInteractionEvent event, String query, String provider){
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        String finalQuery;
        if (isURL(query))
                finalQuery = query;
        else
            finalQuery = String.format("%s: %s", provider, query);

        return Mono.create(monoSink -> PLAYER_MANAGER.loadItem(finalQuery, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                event.editReply(String.format("Adding song `%s` to queue...", audioTrack.getInfo().title)).block();
                play(guildId, audioTrack);
                monoSink.success();
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                if (!audioPlaylist.isSearchResult()){
                    event.editReply(String.format("Adding playlist: `%s` to queue...", audioPlaylist.getName()));
                    for (AudioTrack track : audioPlaylist.getTracks())
                        play(guildId, track);
                    monoSink.success();
                }


                // TODO Create embedded message to show results and select functions
                System.out.println("asdfasdfasdf!");
                for (AudioTrack track : audioPlaylist.getTracks()){
                    System.out.println(track.getInfo().uri);
                }
            }

            @Override
            public void noMatches() {
                event.editReply(String.format("Can't find match to `%s`", finalQuery));
                monoSink.error(new Exception("No match!"));
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.editReply(String.format("Can't play `%s`", finalQuery));
                monoSink.error(e);
            }
        }));
    }


    private void play(Snowflake guildId, AudioTrack track){
        GuildAudioManager.of(guildId).getScheduler().play(track);
    }
}

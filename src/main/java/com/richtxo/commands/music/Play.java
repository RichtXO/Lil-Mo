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
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.voice.AudioProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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

        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(voiceChannel -> {
                    if (voiceChannel == null){
                        event.reply(String.format("%s is not in any voice channels",
                                member.getNicknameMention())).subscribe();
                        return;
                    }

                    String searchQuery = event.getOption("link-or-query")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString).orElse("");
                    String provider = event.getOption("provider")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString).orElse("ytsearch");
                    Snowflake guildId = event.getInteraction().getGuildId().orElse(Snowflake.of(0));
                    AudioProvider voice = GuildAudioManager.of(guildId).getProvider();

                    if (isURL(searchQuery) && searchQuery.toUpperCase().contains("spotify".toUpperCase()))
                        Join.autoDisconnect(voiceChannel, voice)
                                .and(loadSpotifyItem(event, searchQuery, provider)).block();

                    Join.autoDisconnect(voiceChannel, voice).and(loadItem(event, searchQuery, provider)).block();
                })
                .doOnError(t -> event.reply("Something happened..."))
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
            return Mono.create(monoSink -> PLAYER_MANAGER.loadItem(String.format("%s: %s", provider, song)
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
                    event.editReply(String.format("Can't find match to `%s`", song)).block();
                    monoSink.error(new Exception("No match!"));
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    event.editReply(String.format("Can't play `%s`", song)).block();
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
            PLAYER_MANAGER.loadItem(String.format("%s: %s", provider, song), new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    // Shouldn't come in here
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

        return Mono.create(monoSink -> {
            PLAYER_MANAGER.loadItem(finalQuery, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    event.editReply(String.format("Adding song `%s` to queue...", audioTrack.getInfo().title)).block();
                    play(guildId, audioTrack);
                    monoSink.success();
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    if (!audioPlaylist.isSearchResult()) {
                        event.editReply(String.format("Adding playlist: `%s` to queue...", audioPlaylist.getName()));
                        for (AudioTrack track : audioPlaylist.getTracks())
                            play(guildId, track);
                        monoSink.success();
                    }

                    event.editReply("Loading selection menu...").block();
                    EmbedCreateSpec.Builder selectionEmbed = EmbedCreateSpec.builder()
                            .title(String.format("`%s` Music Selection", Objects.requireNonNull(
                                    event.getClient().getSelf().block()).getUsername()))
                            .color(Color.of(0xad0000))
                            .thumbnail(Objects.requireNonNull(
                                    event.getInteraction().getMember().orElse(null)).getAvatarUrl())
                            .description("Select the following by their corresponding numbers.")
                            .addField("\u200B", "", false)
                            .footer("Auto cancels in 10 seconds!",
                                    Objects.requireNonNull(event.getClient().getSelf().block()).getAvatarUrl());
                    for (int i = 0; i < 5; i++)
                        selectionEmbed.addField(String.format("`%d` - %s",
                                i + 1, audioPlaylist.getTracks().get(i).getInfo().title), "", false);

                    Button oneBtn = Button.secondary("1", ReactionEmoji.unicode("1\uFE0F⃣"));
                    Button twoBtn = Button.secondary("2", ReactionEmoji.unicode("2\uFE0F⃣"));
                    Button threeBtn = Button.secondary("3", ReactionEmoji.unicode("3\uFE0F⃣"));
                    Button fourBtn = Button.secondary("4", ReactionEmoji.unicode("4\uFE0F⃣"));
                    Button fiveBtn = Button.secondary("5", ReactionEmoji.unicode("5\uFE0F⃣"));
                    Button cancelBtn = Button.secondary("cancel", ReactionEmoji.unicode("❌"));

                    AtomicBoolean hasSelect = new AtomicBoolean(false);
                    Mono<Void> listener = event.getClient().on(ButtonInteractionEvent.class,
                            buttonEvent -> {
                                String buttonId = buttonEvent.getCustomId();
                                hasSelect.set(true);
                                if (buttonId.equals("cancel"))
                                    return buttonEvent.reply(String.format("Canceling search for %s",
                                            event.getInteraction().getMember().orElse(null)
                                                    .getNicknameMention())).then();

                                play(guildId, audioPlaylist.getTracks().get(Integer.parseInt(buttonId) - 1));
                                return buttonEvent.reply(String.format("Adding `%s` into queue!",
                                                audioPlaylist.getTracks().get(Integer.parseInt(buttonId) - 1).getInfo().title))
                                        .then();

                            })
                    .timeout(Duration.ofSeconds(10))
                    .onErrorResume(ignore -> {
                        if (!hasSelect.get())
                            return event.editReply(String.format(
                                    "Timeout for music selection, %s!",
                                    event.getInteraction().getMember().orElse(null)
                                            .getNicknameMention())).then();
                        return Mono.empty();
                    })
                    .then();

                    event.editReply().withEmbeds(selectionEmbed.build()).withComponents(
                            ActionRow.of(oneBtn, twoBtn, threeBtn, fourBtn, fiveBtn),
                            ActionRow.of(cancelBtn)).then(listener).subscribe();
                    monoSink.success();
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
            });
        });
    }

    private void play(Snowflake guildId, AudioTrack track){
        GuildAudioManager.of(guildId).getScheduler().play(track);
    }
}

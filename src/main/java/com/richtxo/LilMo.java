package com.richtxo;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class LilMo {
    public static final Logger LOGGER = LoggerFactory.getLogger(LilMo.class);

    public static final AudioPlayerManager PLAYER_MANAGER;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize to minimize allocations
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);
    }

    public static void main(String[] args){
        // Checking if all env variables are present
        if (System.getenv("TOKEN") == null) {
            LOGGER.error("Discord Token not set!");
            return;
        }
        if (System.getenv("SPOTIFY_CLIENT_ID") == null || System.getenv("SPOTIFY_SECRET") == null) {
            LOGGER.error("Spotify credentials not set!");
            return;
        }

        final GatewayDiscordClient client = DiscordClientBuilder.create(System.getenv("TOKEN")).build()
                .gateway()
                .setInitialPresence(s -> ClientPresence.online(
                        ClientActivity.listening("/play").withState("Give me more music!")))
                .login().block();

        // Adding commands
        try {
            assert client != null;
            new CommandRegistrar(client.getRestClient()).registerCmds();
        } catch (Exception e) { LOGGER.error("Error trying to register commands: ", e); }

        Mono.when(client.on(ReadyEvent.class).doOnNext(readyEvent -> LOGGER.info("Logged in as {}",
                                        readyEvent.getSelf().getUsername())),
                client.on(ChatInputInteractionEvent.class, Listener::handle),
                client.onDisconnect().doOnTerminate(() -> LOGGER.info("Disconnected!")))
            .block();
    }
}

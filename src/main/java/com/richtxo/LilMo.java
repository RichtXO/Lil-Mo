package com.richtxo;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.common.ReactorResources;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.request.RouteMatcher;
import discord4j.rest.response.ResponseFunction;
import io.netty.channel.unix.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.resources.ConnectionProvider;
import reactor.retry.Retry;

import java.io.IOException;
import java.time.Duration;

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
        final GatewayDiscordClient client = DiscordClientBuilder.create(System.getenv("TOKEN"))
                .onClientResponse(ResponseFunction.retryWhen(RouteMatcher.any(),
                        Retry.anyOf(Errors.NativeIoException.class)))
//                .setReactorResources(ReactorResources.builder()
//                        .httpClient(ReactorResources.newHttpClient(ConnectionProvider.builder("custom")
//                                .maxIdleTime(Duration.ofMinutes(10))
//                                .build()))
//                        .build())
                .build()
                .gateway()
                .setInitialPresence(s -> ClientPresence.online(
                        ClientActivity.listening("/play").withState("Give me more music!")))
                .login()
                .block();

        // Adding commands
        try {
            assert client != null;
            new CommandRegistrar(client.getRestClient()).registerCmds();
        } catch (Exception e) {
            LOGGER.error("Error trying to register commands", e);
        }

        client.on(ChatInputInteractionEvent.class, Listener::handle)
                .then(client.onDisconnect())
                .block();
    }
}

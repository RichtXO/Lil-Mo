package com.richtxo;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
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
        final DiscordClient client = DiscordClient.create(System.getenv("TOKEN"));
        client.gateway().setInitialPresence(s -> ClientPresence.online(
                ClientActivity.listening("/play").withState("Give me more music!")));

        Mono<Void> login = client.withGateway(gatewayDiscordClient -> {
            Mono<Void> printOnLogin = gatewayDiscordClient.on(ReadyEvent.class, event ->
                            Mono.fromRunnable(() -> {
                                final User self = event.getSelf();
                                LOGGER.debug("Logged in as {}", self.getUsername());
                            }))
                    .then();

            // Adding commands
            try { new CommandRegistrar(client).registerCmds(); }
            catch (Exception e) { LOGGER.error("Error trying to register commands: ", e); }

            Mono<Void> handleCommand = gatewayDiscordClient.on(ChatInputInteractionEvent.class, Listener::handle)
                    .then();

            return printOnLogin.and(handleCommand);
        });

        login.block();
    }
}

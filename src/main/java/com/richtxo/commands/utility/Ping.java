package com.richtxo.commands.utility;

import com.richtxo.commands.Command;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.gateway.GatewayClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

public class Ping implements Command {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public String getCmdInfo() {
        return "Displays Lil Mo's latency to Discord API";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Optional<Duration> latency = event.getClient().getGatewayClient(event.getShardInfo().getIndex())
                .map(GatewayClient::getResponseTime);

        return latency.map(duration ->
                event.reply(String.format("API Latency: `%d ms`", duration.toMillis()))).
                orElseGet(() -> event.reply("API Latency: N/A"));
    }
}

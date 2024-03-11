package com.richtxo.commands.music;

import com.richtxo.commands.Command;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

public class Queue implements Command {
    @Override
    public String getName() {
        return "queue";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Display list of songs in queue";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        //TODO




        return Command.super.handle(event);
    }
}

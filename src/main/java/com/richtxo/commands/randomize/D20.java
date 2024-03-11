package com.richtxo.commands.randomize;

import com.richtxo.commands.Command;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Random;

public class D20 implements Command {
    @Override
    public String getName() {
        return "d20";
    }

    @Override
    public String getCategory() {
        return "Randomize";
    }

    @Override
    public String getCmdInfo() {
        return "Roll a d20 dice";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String user = Objects.requireNonNull(event.getInteraction().getMember().orElse(null)).getNicknameMention();
        return event.reply().withContent(String.format("%s has rolled a `%d`", user, new Random().nextInt(20) + 1));
    }
}

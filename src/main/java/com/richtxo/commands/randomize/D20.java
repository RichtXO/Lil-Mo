package com.richtxo.commands.randomize;

import module java.base;
import com.richtxo.commands.Command;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

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
        String user = event.getInteraction().getMember()
                .map(Member::getNicknameMention)
                .orElse("`N/A`");

        return event.reply("%s has rolled a `%d`".formatted(user, ThreadLocalRandom.current().nextInt(1, 21)));
    }
}

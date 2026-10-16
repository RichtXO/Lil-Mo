package com.richtxo.commands.randomize;

import module java.base;
import com.richtxo.commands.Command;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

public class CoinFlip implements Command {
    @Override
    public String getName() {
        return "coin-flip";
    }

    @Override
    public String getCategory() {
        return "Randomize";
    }

    @Override
    public String getCmdInfo() {
        return "Coin flipping";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String user = event.getInteraction().getMember()
                .map(Member::getNicknameMention)
                .orElse("`N/A`");

        if (ThreadLocalRandom.current().nextBoolean())
            return event.reply("@%s flipped `Heads`!".formatted(user)).then();

        return event.reply("%s flipped `Tails`!".formatted(user)).then();
    }
}

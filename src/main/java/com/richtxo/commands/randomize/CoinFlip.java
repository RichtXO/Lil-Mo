package com.richtxo.commands.randomize;

import com.richtxo.commands.Command;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;
import java.util.Random;

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
        String user = ((event.getInteraction().getMember().isPresent()) ?
                event.getInteraction().getMember().get().getNicknameMention() : "`N/A`");

        if (new Random().nextInt(2) == 0)
            return event.reply(String.format("@%s flipped `Heads`!", user)).then();

        return event.reply(String.format("%s flipped `Tails`!", user)).then();
    }
}

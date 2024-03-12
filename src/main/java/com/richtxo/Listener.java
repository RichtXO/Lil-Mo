package com.richtxo;

import com.richtxo.commands.Command;
import com.richtxo.commands.music.*;
import com.richtxo.commands.randomize.*;
import com.richtxo.commands.utility.*;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class Listener {
    private final static List<Command> commands = new ArrayList<>();

    static {
        // Adding cmds here
        commands.add(new Ping());

        // Music Commands
        commands.add(new Join());
        commands.add(new Play());
        commands.add(new Leave());
        commands.add(new Resume());
        commands.add(new Pause());
        commands.add(new Skip());
        commands.add(new Shuffle());

        // Randomize Commands
        commands.add(new CoinFlip());
        commands.add(new D20());
    }

    public static Mono<Void> handle(ChatInputInteractionEvent event){
        return Flux.fromIterable(commands)
                .filter(command -> command.getName().equals(event.getCommandName()))
                .next()
                .flatMap(command -> command.handle(event));
    }
}

package com.richtxo;

import com.richtxo.commands.Command;
import com.richtxo.commands.randomize.D20;
import com.richtxo.commands.randomize.CoinFlip;
import com.richtxo.commands.music.Join;
import com.richtxo.commands.music.Leave;
import com.richtxo.commands.music.Play;
import com.richtxo.commands.utility.Ping;
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

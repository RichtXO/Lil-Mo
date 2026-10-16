package com.richtxo;

import module java.base;
import com.richtxo.commands.Command;
import com.richtxo.commands.music.Join;
import com.richtxo.commands.music.Leave;
import com.richtxo.commands.music.Pause;
import com.richtxo.commands.music.Play;
import com.richtxo.commands.music.Queue;
import com.richtxo.commands.music.Repeat;
import com.richtxo.commands.music.RepeatOff;
import com.richtxo.commands.music.Resume;
import com.richtxo.commands.music.Shuffle;
import com.richtxo.commands.music.Skip;
import com.richtxo.commands.music.Stop;
import com.richtxo.commands.randomize.CoinFlip;
import com.richtxo.commands.randomize.D20;
import com.richtxo.commands.utility.Ping;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Listener {
    private static final List<Command> commands = List.of(
            // Adding commands here
            new Ping(),

            // Music Commands
            new Join(),
            new Play(),
            new Leave(),
            new Resume(),
            new Pause(),
            new Skip(),
            new Stop(),
            new Shuffle(),
            new Queue(),
            new Repeat(),
            new RepeatOff(),

            // Randomize Commands
            new CoinFlip(),
            new D20()
    );

    public static Mono<Void> handle(ChatInputInteractionEvent event) {
        return Flux.fromIterable(commands)
                .filter(command -> command.getName().equals(event.getCommandName()))
                .next()
                .flatMap(command -> command.handle(event));
    }
}

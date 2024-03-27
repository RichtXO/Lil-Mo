package com.richtxo.commands.music;

import com.richtxo.commands.Command;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Leave implements Command {
    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Makes Lil Mo leave current voice channel";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(voiceChannel -> {
                    if (voiceChannel == null){
                        event.reply(String.format("%s must join a voice channel first!",
                                member.getNicknameMention())).subscribe();
                        return;
                    }

                    voiceChannel.getVoiceConnection()
                            .publishOn(Schedulers.boundedElastic())
                            .doOnSuccess(voiceConnection -> {
                                if (voiceConnection == null) {
                                    voiceChannel.sendDisconnectVoiceState()
                                            .then(event.reply("Not in any voice channel!"))
                                            .subscribe();
                                    return;
                                }
                            })
                            .flatMap(VoiceConnection::disconnect)
                            .then(event.reply(String.format("Leaving `\uD83d\uDD0A %s`!", voiceChannel.getName())))
                            .subscribe();
                })
                .doOnError(t -> event.reply("Something happened..."))
                .then();
    }
}

package com.richtxo.commands.music;

import module java.base;
import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Join implements Command {
    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getCategory() {
        return "Music";
    }

    @Override
    public String getCmdInfo() {
        return "Makes Lil Mo join your current voice channel";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Member member = event.getInteraction().getMember().get();

        return member.getVoiceState()
                .flatMap(VoiceState::getChannel)
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(voiceChannel -> {
                    if (voiceChannel == null) {
                        event.reply("%s must join a voice channel first!".formatted(member.getNicknameMention()))
                                .subscribe();
                        return;
                    }
                    AudioProvider voice = GuildAudioManager.of(member.getGuildId()).getProvider();
                    event.reply("Joining `\uD83d\uDD0A %s`!".formatted(voiceChannel.getName()))
                            .then(autoDisconnect((VoiceChannel) voiceChannel, voice)).subscribe();
                })
                .doOnError(_ -> event.reply("Something happened..."))
                .then();
    }

    public static Mono<Void> autoDisconnect(VoiceChannel channel, AudioProvider voice) {
        return channel.join().withProvider(voice)
                .flatMap(voiceConnection -> {
                    // The bot itself has a VoiceState; 1 VoiceState signals bot is alone
                    Publisher<Boolean> voiceStateCounter = channel.getVoiceStates()
                            .count()
                            .map(count -> 1L == count);

                    // After 5 seconds, check if the bot is alone. This is useful if
                    // the bot joined alone, but no one else joined since connecting
                    Mono<Void> onDelay = Mono.delay(Duration.ofSeconds(5L))
                            .filterWhen(_ -> voiceStateCounter)
                            .switchIfEmpty(Mono.never())
                            .then();

                    // As people join and leave `channel`, check if the bot is alone.
                    Mono<Void> onEvent = channel.getClient().getEventDispatcher().on(VoiceStateUpdateEvent.class)
                            .filter(event -> event.getOld().flatMap(VoiceState::getChannelId)
                                    .map(channel.getId()::equals).orElse(false))
                            .filterWhen(_ -> voiceStateCounter)
                            .next()
                            .then();

                    // Disconnect the bot if either onDelay or onEvent are completed!
                    return onDelay.or(onEvent).then(voiceConnection.disconnect());
                });
    }
}

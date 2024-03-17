package com.richtxo.commands.music;

import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        List<AudioTrack> musicQueue = GuildAudioManager.of(guildId).getScheduler().getQueue();
        User bot = Objects.requireNonNull(event.getClient().getSelf().block());
        AudioTrackInfo current = GuildAudioManager.of(guildId).getPlayer().getPlayingTrack().getInfo();

        if (musicQueue.isEmpty()){
            if (GuildAudioManager.of(guildId).getPlayer().getPlayingTrack() == null)
                return event.reply("No songs in queue!");

            long totalHour = (current.length / (1000 * 60 * 60)) % 24;
            long totalMin = (current.length / (1000 * 60)) % 60;
            long totalSec = (current.length / 1000) % 60;

            EmbedCreateSpec.Builder currentSong = EmbedCreateSpec.builder()
                    .title(String.format("Current song: %s", current.title))
                    .color(Color.of(0xad0000))
                    .thumbnail((current.artworkUrl==null ? bot.getAvatarUrl() : current.artworkUrl))
                    .addField("Author", (current.author != null ? current.author : "N/A"), true)
                    .addField("ISRC", (current.isrc != null ? current.isrc : "N/A"), true)
                    .addField("URI", (current.uri != null ? current.uri : "N/A"), false)
                    .footer(String.format("Total Time: %02d:%02d:%02d", totalHour, totalMin, totalSec),
                            bot.getAvatarUrl());

            return event.reply().withEmbeds(currentSong.build());
        }

        long totalTime = current.length;
        List<EmbedCreateSpec.Builder> paginationsBuilder = new ArrayList<>();
        for (int pagination = 0; pagination < musicQueue.size() / 10; pagination++){
            EmbedCreateSpec.Builder builder = getQueueBase(event, musicQueue, current);
            for (int i = 0; i < Math.min(musicQueue.size() - (pagination * 10), 10); i++){
                AudioTrackInfo track = musicQueue.get(i + (pagination * 10)).getInfo();
                totalTime += track.length;
                long min = (track.length / (1000 * 60)) % 60;
                long sec = (track.length / 1000) % 60;
                builder.addField(String.format("`%d.` `[%02d:%02d]` %s", i + 1, min, sec, track.title),
                        "", false);
            }
            paginationsBuilder.add(builder);
        }

        List<EmbedCreateSpec> paginations = getBuilders(totalTime, paginationsBuilder, bot);
        Button nextBtn = Button.secondary("next", ReactionEmoji.unicode("⏭️"));
        Button prevBtn = Button.secondary("prev", ReactionEmoji.unicode("⏮️"));
        Button doneDtn = Button.secondary("done", ReactionEmoji.unicode("✅"));

        AtomicInteger page = new AtomicInteger();
        Mono<Void> buttonListener = event.getClient().on(ButtonInteractionEvent.class, buttonEvent -> {
            String buttonId = buttonEvent.getCustomId();
            if (buttonId.equals("next")){
                page.addAndGet(1);
                return buttonEvent.reply().withEmbeds(paginations.get(page.get())).withComponents(
                        ActionRow.of(prevBtn, nextBtn.disabled(page.get() == paginations.size() - 1), doneDtn));
            }
            if (buttonId.equals("prev")){
                page.decrementAndGet();
                return buttonEvent.reply().withEmbeds(paginations.get(page.get())).withComponents(
                        ActionRow.of(prevBtn.disabled(page.get() == 0), nextBtn, doneDtn));
            }

            // When done button is pressed
            return buttonEvent.reply(String.format("Exiting out of queue, %s!",
                    Objects.requireNonNull(buttonEvent.getInteraction().getMember().orElse(null))
                            .getNicknameMention()));
        }).then();

        return event.reply()
                .withEmbeds(paginations.getFirst())
                .withComponents(ActionRow.of(prevBtn.disabled(), nextBtn, doneDtn))
                .then(buttonListener);
    }

    private EmbedCreateSpec.Builder getQueueBase(ChatInputInteractionEvent event, List<AudioTrack> musicQueue,
                                                 AudioTrackInfo current) {
        return EmbedCreateSpec.builder()
                .title(String.format("`%d` Total Songs in Queue", musicQueue.size() + 1))
                .color(Color.of(0xad0000))
                .description(String.format("Current song: `%s`", current.title))
                .thumbnail(Objects.requireNonNull(
                        event.getInteraction().getMember().orElse(null)).getAvatarUrl());
    }

    private static List<EmbedCreateSpec> getBuilders(
            long totalTime, List<EmbedCreateSpec.Builder> paginationsBuilder, User bot) {
        
        long totalHour = (totalTime / (1000 * 60 * 60)) % 24;
        long totalMin = (totalTime / (1000 * 60)) % 60;
        long totalSec = (totalTime / 1000) % 60;

        List<EmbedCreateSpec> paginations = new ArrayList<>();
        for (int i = 0; i < paginationsBuilder.size(); i++){
            EmbedCreateSpec.Builder page = paginationsBuilder.get(i);
            page.footer(String.format("Total Time: [%02d:%02d:%02d] -- Page %d / %d",
                    totalHour, totalMin, totalSec, i + 1, paginationsBuilder.size()), bot.getAvatarUrl());
            paginations.add(page.build());
        }
        return paginations;
    }
}

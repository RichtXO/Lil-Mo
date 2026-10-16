package com.richtxo.commands.music;

import module java.base;
import com.richtxo.audio.GuildAudioManager;
import com.richtxo.commands.Command;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.emoji.Emoji;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

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
        return event.getClient().getSelf()
                .flatMap(user -> {
                    Snowflake guildId = event.getInteraction().getGuildId().orElse(Snowflake.of(0));
                    List<AudioTrack> musicQueue = GuildAudioManager.of(guildId).getScheduler().getQueue();

                    if (musicQueue.isEmpty()) {
                        if (GuildAudioManager.of(guildId).getPlayer().getPlayingTrack() == null)
                            return event.reply("No songs in queue!");

                        AudioTrackInfo current = GuildAudioManager.of(guildId).getPlayer().getPlayingTrack().getInfo();
                        long totalHour = (current.length / (1000 * 60 * 60)) % 24;
                        long totalMin = (current.length / (1000 * 60)) % 60;
                        long totalSec = (current.length / 1000) % 60;

                        EmbedCreateSpec.Builder currentSong = EmbedCreateSpec.builder()
                                .title("Current song: %s".formatted(current.title))
                                .color(Color.of(0xad0000))
                                .thumbnail(user.getAvatarUrl())
                                .addField("Author", Objects.requireNonNullElse(current.author, "N/A"), true)
                                .addField("URI", Objects.requireNonNullElse(current.uri, "N/A"), false)
                                .footer("Total Time: %02d:%02d:%02d".formatted(totalHour, totalMin, totalSec),
                                        user.getAvatarUrl());

                        return event.reply().withEmbeds(currentSong.build());
                    }

                    AudioTrackInfo current = GuildAudioManager.of(guildId).getPlayer().getPlayingTrack().getInfo();
                    long totalTime = current.length;
                    List<EmbedCreateSpec.Builder> paginationBuilder = new ArrayList<>();
                    for (int pagination = 0; pagination < Math.ceilDiv(musicQueue.size(), 10); pagination++) {
                        EmbedCreateSpec.Builder builder = getQueueBase(event, musicQueue, current);

                        for (int i = 0; i < Math.min(
                                Math.min(musicQueue.size(), musicQueue.size() - (pagination * 10)), 10); i++) {
                            AudioTrackInfo track = musicQueue.get(i + (pagination * 10)).getInfo();
                            totalTime += track.length;
                            long min = (track.length / (1000 * 60)) % 60;
                            long sec = (track.length / 1000) % 60;
                            builder.addField("`%d.` `[%02d:%02d]` %s".formatted(i + 1, min, sec, track.title),
                                    "", false);
                        }
                        paginationBuilder.add(builder);
                    }

                    List<EmbedCreateSpec> pagination = getBuilders(totalTime, paginationBuilder, user);
                    Button nextBtn = Button.secondary("next", Emoji.unicode("\u23ed\ufe0f"));
                    Button prevBtn = Button.secondary("prev", Emoji.unicode("\u23ee\ufe0f"));
                    Button doneDtn = Button.secondary("done", Emoji.unicode("\u2705"));

                    AtomicInteger page = new AtomicInteger();
                    AtomicBoolean ifDone = new AtomicBoolean(false);
                    Mono<Void> buttonListener = event.getClient().on(ButtonInteractionEvent.class, buttonEvent ->
                                    switch (buttonEvent.getCustomId()) {
                                        case "next" -> {
                                            int currentPage = page.incrementAndGet();
                                            yield buttonEvent.reply().withEmbeds(pagination.get(currentPage)).withComponents(
                                                    ActionRow.of(prevBtn,
                                                            nextBtn.disabled(currentPage == pagination.size() - 1), doneDtn));
                                        }
                                        case "prev" -> {
                                            int currentPage = page.decrementAndGet();
                                            yield buttonEvent.reply().withEmbeds(pagination.get(currentPage)).withComponents(
                                                    ActionRow.of(prevBtn.disabled(currentPage == 0), nextBtn, doneDtn));
                                        }
                                        default -> {
                                            // When done button is pressed
                                            ifDone.set(true);
                                            yield buttonEvent.reply("Exiting out of queue, %s!".formatted(
                                                    Objects.requireNonNull(buttonEvent.getInteraction().getMember().orElse(null))
                                                            .getNicknameMention()));
                                        }
                                    })
                            .timeout(Duration.ofSeconds(30))
                            .onErrorResume(_ -> {
                                if (!ifDone.get())
                                    return event.editReply("Timeout for queue, %s!".formatted(
                                            event.getInteraction().getMember().get()
                                                    .getNicknameMention())).then();
                                return Mono.empty();
                            }).then();

                    return event.reply()
                            .withEmbeds(pagination.getFirst())
                            .withComponents(ActionRow.of(prevBtn.disabled(),
                                    (page.get() == pagination.size() - 1 ? nextBtn.disabled() : nextBtn), doneDtn))
                            .then(buttonListener);
                });
    }

    private EmbedCreateSpec.Builder getQueueBase(ChatInputInteractionEvent event, List<AudioTrack> musicQueue,
                                                 AudioTrackInfo current) {
        return EmbedCreateSpec.builder()
                .title("`%d` Total Songs in Queue".formatted(musicQueue.size() + 1))
                .color(Color.of(0xad0000))
                .description("Current song: `%s`".formatted(current.title))
                .thumbnail(event.getInteraction().getMember()
                        .map(Member::getAvatarUrl)
                        .orElse(""));
    }

    private static List<EmbedCreateSpec> getBuilders(
            long totalTime, List<EmbedCreateSpec.Builder> paginationsBuilder, User bot) {

        long totalHour = (totalTime / (1000 * 60 * 60)) % 24;
        long totalMin = (totalTime / (1000 * 60)) % 60;
        long totalSec = (totalTime / 1000) % 60;

        List<EmbedCreateSpec> pagination = new ArrayList<>();
        for (int i = 0; i < paginationsBuilder.size(); i++) {
            EmbedCreateSpec.Builder page = paginationsBuilder.get(i);
            page.footer("Total Time: [%02d:%02d:%02d] -- Page %d / %d".formatted(
                    totalHour, totalMin, totalSec, i + 1, paginationsBuilder.size()), bot.getAvatarUrl());
            pagination.add(page.build());
        }
        return pagination;
    }
}

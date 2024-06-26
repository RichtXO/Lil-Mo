package com.richtxo.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {

    private final List<AudioTrack> queue;
    private final AudioPlayer player;
    private boolean repeat = false;

    public TrackScheduler(AudioPlayer player) {
        // The queue may be modified by different threads so guarantee memory safety
        // This does not, however, remove several race conditions currently present
        queue = Collections.synchronizedList(new LinkedList<>());
        this.player = player;
    }

    public List<AudioTrack> getQueue() {
        return queue;
    }

    public boolean play(AudioTrack track) {
        return play(track, false);
    }

    public boolean play(AudioTrack track, boolean force) {
        boolean playing = player.startTrack(track, !force);
        if (!playing)
            queue.add(track);
        return playing;
    }

    public boolean skip() {
        return !queue.isEmpty() && play(queue.removeFirst(), true);
    }

    public boolean skip(int index){
        if (!queue.isEmpty()){
            queue.remove(index);
            return true;
        }
        return false;
    }

    public void clear() {
        this.queue.clear();
    }

    public void shuffle() {
        Collections.shuffle(queue);
    }

    public void setRepeating(boolean repeating){
        this.repeat = repeating;
    }

    public boolean isRepeating(){
        return this.repeat;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Advance the player if the track completed naturally (FINISHED) or if the track cannot play (LOAD_FAILED)
        if (endReason.mayStartNext) {
            if (repeat)
                player.startTrack(track.makeClone(), false);
            else
                skip();
        }
    }
}
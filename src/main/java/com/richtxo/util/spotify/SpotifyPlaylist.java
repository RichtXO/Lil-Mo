package com.richtxo.util.spotify;

import module java.base;

public record SpotifyPlaylist(String title, SpotifySong[] songs) {

    public String getTitle() {
        return this.title;
    }

    public SpotifySong[] getSongs() {
        return this.songs;
    }

    public String[] toStringSongs() {
        return Arrays.stream(songs)
                .map(SpotifySong::toString)
                .toArray(String[]::new);
    }
}

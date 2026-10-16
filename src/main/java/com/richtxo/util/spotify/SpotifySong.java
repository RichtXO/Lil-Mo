package com.richtxo.util.spotify;

import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;

public record SpotifySong(String title, ArtistSimplified[] artists) {

    public SpotifySong() {
        this("", new ArtistSimplified[]{});
    }

    public String getTitle() {
        return this.title;
    }

    public ArtistSimplified[] getArtist() {
        return this.artists;
    }

    @Override
    public String toString() {
        StringBuilder artistNames = new StringBuilder();
        for (ArtistSimplified artist : this.artists)
            artistNames.append("%s ".formatted(artist.getName()));

        return "%s %s lyrics".formatted(this.title, artistNames);
    }
}

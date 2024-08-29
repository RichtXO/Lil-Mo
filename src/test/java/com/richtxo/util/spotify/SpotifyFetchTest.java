package com.richtxo.util.spotify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyFetchTest {

    private final SpotifyFetch fetch = new SpotifyFetch();

    @Test
    @DisabledIfEnvironmentVariable(named = "SPOTIFY_CLIENT_ID", matches = "", disabledReason = "Needs Spotify Client ID")
    @DisabledIfEnvironmentVariable(named = "SPOTIFY_SECRET", matches = "", disabledReason = "Needs Spotify Secret")
    void fetchSong() {
        final SpotifySong valid =
                fetch.fetchSong("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT?si=8347518e3fd249f5");
        assertNotNull(valid);
        assertEquals("Never Gonna Give You Up", valid.getTitle());
        assertEquals("Rick Astley", valid.getArtist()[0].getName());

        assertNull(fetch.fetchSong("not_valid_spotify_link"));
        assertNull(fetch.fetchSong(null));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SPOTIFY_CLIENT_ID", matches = "", disabledReason = "Needs Spotify Client ID")
    @DisabledIfEnvironmentVariable(named = "SPOTIFY_SECRET", matches = "", disabledReason = "Needs Spotify Secret")
    void fetchPlaylist() {
        final SpotifyPlaylist valid =
                fetch.fetchPlaylist("https://open.spotify.com/playlist/51mPlHVdm7RAqdMACrOaXO?si=9ffd4363a75f4c41");
        assertNotNull(valid);
        assertEquals("Nostalgia", valid.getTitle());
        assertNotEquals(0, valid.getSongs().length);

        assertNull(fetch.fetchPlaylist(null));
        assertNull(fetch.fetchPlaylist("not_valid_spotify_link"));
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SPOTIFY_CLIENT_ID", matches = "", disabledReason = "Needs Spotify Client ID")
    @DisabledIfEnvironmentVariable(named = "SPOTIFY_SECRET", matches = "", disabledReason = "Needs Spotify Secret")
    void fetchAlbum() {
        final SpotifyPlaylist valid =
                fetch.fetchAlbum("https://open.spotify.com/album/5Z9iiGl2FcIfa3BMiv6OIw?si=QdYDFg5nSDmoi5grrwHH7g");
        assertNotNull(valid);
        assertEquals("Whenever You Need Somebody", valid.getTitle());
        assertEquals(10, valid.getSongs().length);

        assertNull(fetch.fetchAlbum(null));
        assertNull(fetch.fetchAlbum("not_valid_spotify_link"));
    }
}
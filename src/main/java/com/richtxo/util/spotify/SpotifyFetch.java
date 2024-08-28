package com.richtxo.util.spotify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class SpotifyFetch {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final SpotifyApi spotify;

    public SpotifyFetch() {
        this.spotify = new SpotifyApi.Builder()
                .setClientId(System.getenv("SPOTIFY_CLIENT_ID"))
                .setClientSecret(System.getenv("SPOTIFY_SECRET"))
                .build();

        try {
            ClientCredentialsRequest.Builder request = new ClientCredentialsRequest.Builder
                    (spotify.getClientId(), spotify.getClientSecret());
            ClientCredentials creds = request.grant_type("client_credentials").build().execute();
            spotify.setAccessToken(creds.getAccessToken());
        } catch (Exception e){ LOGGER.error("Error init spotify api: {}", e.getMessage()); }
    }


    public SpotifySong fetchSong (String url){
        String id = getID(url);
        if (id.isEmpty()){
            LOGGER.error("Error when fetching spotify song");
            return null;
        }

        CompletableFuture<Track> trackFuture = spotify.getTrack(id).build().executeAsync();
        Track track = trackFuture.join();
        return new SpotifySong(track.getName(), track.getArtists());
    }

    public SpotifyPlaylist fetchPlaylist (String url){
        String id = getID(url);
        if (id.isEmpty()){
            LOGGER.error("Error when fetching spotify playlist");
            return null;
        }

        CompletableFuture<Playlist> playlistFuture = spotify.getPlaylist(id).build().executeAsync();
        List<String> songIDs = new ArrayList<>();
        Playlist playlist = playlistFuture.join();
        for (PlaylistTrack track : playlist.getTracks().getItems())
            songIDs.add(track.getTrack().getId());

        List<Track> tracks = new ArrayList<>();
        if (songIDs.size() > 50){
            for (int i = 1; i <= Math.ceil((double) songIDs.size() / 50); i++){
                String test = String.join(",", songIDs.subList(50 * (i - 1),
                        Math.min(50 * i, songIDs.size())));
                CompletableFuture<Track[]> trackFuture = spotify.getSeveralTracks(test).build().executeAsync();
                Track[] temp = trackFuture.join();
                Collections.addAll(tracks, temp);
            }
        }

        List<SpotifySong> songs = new ArrayList<>();
        for (Track track : tracks)
            songs.add(new SpotifySong(track.getName(), track.getArtists()));

        return new SpotifyPlaylist(playlist.getName(), songs.toArray(SpotifySong[]::new));
    }

    public SpotifyPlaylist fetchAlbum (String url){
        String id = getID(url);
        if (id.isEmpty()){
            LOGGER.error("Error when fetching spotify album");
            return null;
        }

        List<SpotifySong> songs = new ArrayList<>();
        CompletableFuture<Album> albumFuture = spotify.getAlbum(id).build().executeAsync();
        Album album = albumFuture.join();
        for (TrackSimplified track : album.getTracks().getItems())
            songs.add(new SpotifySong(track.getName(), track.getArtists()));

        return new SpotifyPlaylist(album.getName(), songs.toArray(SpotifySong[]::new));
    }

    private String getID(String link){
        if (link == null || !ifValidSpotifyLink(link))
            return "";

        if (link.contains("?"))
            return link.substring(link.lastIndexOf("/") + 1, link.indexOf("?"));

        return link.substring(link.lastIndexOf("/"));
    }

    private boolean ifValidSpotifyLink(String link){
        Pattern spotifyPattern = Pattern.compile("^(spotify:|https://[a-z]+\\.spotify\\.com/)");
        return spotifyPattern.matcher(link).find();
    }
}

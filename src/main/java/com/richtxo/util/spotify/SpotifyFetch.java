package com.richtxo.util.spotify;

import ch.qos.logback.core.boolex.Matcher;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.richtxo.LilMo.LOGGER;
public class SpotifyFetch {

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
        } catch (Exception e){
            LOGGER.error("Error init spotify api: " + e.getMessage());
        }
    }


    public SpotifySong fetchSong (String url){
        String test = getID(url);
        try{
            String id = getID(url);
            Track track = spotify.getTrack(id).build().execute();
            return new SpotifySong(track.getName(), track.getArtists());
        } catch (Exception e) {
            LOGGER.error(String.format("Error when fetching spotify song `%s`: %s", getID(url), e.getMessage()));
            return null;
        }
    }

    public SpotifyPlaylist fetchPlaylist (String url){
        try {
            List<String> songIDs = new ArrayList<>();
            Playlist playlist = spotify.getPlaylist(getID(url)).build().execute();
            for (PlaylistTrack track : playlist.getTracks().getItems())
                songIDs.add(track.getTrack().getId());

            List<SpotifySong> songs = new ArrayList<>();
            Track[] tracks = spotify.getSeveralTracks(songIDs.toArray(String[]::new)).build().execute();
            for (Track track : tracks)
                songs.add(new SpotifySong(track.getName(), track.getArtists()));

            return new SpotifyPlaylist(playlist.getName(), songs.toArray(SpotifySong[]::new));

        } catch (Exception e){
            LOGGER.error(String.format("Error when fetching spotify playlist `%s`: %s", getID(url), e.getMessage()));
        }
        return null;
    }

    public SpotifyPlaylist fetchAlbum (String url){
        try{
            List<SpotifySong> songs = new ArrayList<>();
            Album album = spotify.getAlbum(getID(url)).build().execute();
            for (TrackSimplified track : album.getTracks().getItems())
                songs.add(new SpotifySong(track.getName(), track.getArtists()));

            return new SpotifyPlaylist(album.getName(), songs.toArray(SpotifySong[]::new));
        } catch (Exception e){
            LOGGER.error(String.format("Error when fetching spotify album `%s`: %s", getID(url), e.getMessage()));
        }
        return null;
    }

    private String getID(String link){
        if (ifValidSpotifyLink(link)){
            if (link.contains("?"))
                return link.substring(link.lastIndexOf("/") + 1, link.indexOf("?"));
            return link.substring(link.lastIndexOf("/"));
        }
        return "";
    }

    private boolean ifValidSpotifyLink(String link){
        Pattern spotifyPattern = Pattern.compile("^(spotify:|https://[a-z]+\\.spotify\\.com/)");
        return spotifyPattern.matcher(link).find();
    }
}

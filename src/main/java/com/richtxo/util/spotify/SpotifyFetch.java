package com.richtxo.util.spotify;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.richtxo.LilMo.LOGGER;
public class SpotifyFetch {

    private final SpotifyApi spotify;
    private final int LIMIT = 50;

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


            List<Track> tracks = new ArrayList<>();
            if (songIDs.size() > LIMIT){
                for (int i = 1; i <= Math.ceil((double) songIDs.size() / LIMIT); i++){
                    String test = String.join(",", songIDs.subList(LIMIT * (i - 1), Math.min(LIMIT * i, songIDs.size())));
                    Track[] temp = spotify.getSeveralTracks(test).build().execute();
                    Collections.addAll(tracks, temp);
                }
            }

            List<SpotifySong> songs = new ArrayList<>();
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

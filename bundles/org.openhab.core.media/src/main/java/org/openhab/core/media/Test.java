package org.openhab.core.media;

import org.openhab.core.media.internal.MediaServiceImpl;
import org.openhab.core.media.model.MediaAlbum;
import org.openhab.core.media.model.MediaArtist;
import org.openhab.core.media.model.MediaCollection;
import org.openhab.core.media.model.MediaRegistry;
import org.openhab.core.media.model.MediaEntrySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {
    private final Logger logger = LoggerFactory.getLogger(Test.class);

    public static void main(String args[]) {

        new Test().Go();
    }

    public void Go() {
        MediaService mediaService = new MediaServiceImpl();
        MediaRegistry mediaRegistry = mediaService.getMediaRegistry();

        MediaEntrySupplier mediaSource = mediaRegistry.registerEntry("Spotify", () -> {
            return new MediaEntrySupplier("Spotify", "SpotifyName");
        });

        MediaCollection mediaAlbums = mediaSource.registerEntry("Albums", () -> {
            return new MediaCollection("Albums", "Albums");
        });

        MediaCollection mediaArtists = mediaSource.registerEntry("Artists", () -> {
            return new MediaCollection("Artists", "Artists");
        });

        @SuppressWarnings("unused")
        MediaCollection mediaPlaylist = mediaSource.registerEntry("Playlists", () -> {
            return new MediaCollection("Playlists", "Playlists");
        });

        @SuppressWarnings("unused")
        MediaAlbum mediaAlbum = mediaAlbums.registerEntry("Album_1", () -> {
            return new MediaAlbum("Album_1", "Another day to die");
        });

        @SuppressWarnings("unused")
        MediaAlbum mediaAlbum2 = mediaAlbums.registerEntry("Album_2", () -> {
            return new MediaAlbum("Album_2", "Samedi soir sur terre");
        });

        @SuppressWarnings("unused")
        MediaArtist mediaArtiste = mediaArtists.registerEntry("Artist_1", () -> {
            return new MediaArtist("Artist_1", "Dire Straits");
        });

        @SuppressWarnings("unused")
        MediaArtist mediaArtiste2 = mediaArtists.registerEntry("Artist_2", () -> {
            return new MediaArtist("Artist_2", "Francis Cabrel");
        });

        mediaRegistry.print();

        logger.debug("");

    }

}

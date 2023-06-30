package taha.younis.musicappjava.data;

import android.net.Uri;

public class Song {
    String title;
    Uri uri;
    Uri artworkUri;
    String albumUriForImage;
    int size;
    int duration;

    public Song(String title, Uri uri, Uri artworkUri, String albumUriForImage, int size, int duration) {
        this.title = title;
        this.uri = uri;
        this.artworkUri = artworkUri;
        this.albumUriForImage = albumUriForImage;
        this.size = size;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getArtworkUri() {
        return artworkUri;
    }

    public String getAlbumUriForImage() {
        return albumUriForImage;
    }

    public int getSize() {
        return size;
    }

    public int getDuration() {
        return duration;
    }
}

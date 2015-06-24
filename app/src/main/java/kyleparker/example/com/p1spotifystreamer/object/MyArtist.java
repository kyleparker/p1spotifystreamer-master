package kyleparker.example.com.p1spotifystreamer.object;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Custom {@link Artist} object used to implement Parcelable.
 *
 * Created by kyleparker on 6/23/2015.
 */
public class MyArtist extends Artist implements Parcelable {
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public MyArtist() { }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        try {
            parcel.writeString(id);
            parcel.writeString(name);
            parcel.writeString(imageUrl);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private MyArtist(Parcel source) {
        id = source.readString();
        name = source.readString();
        imageUrl = source.readString();
    }

    public static final Creator<MyArtist> CREATOR = new Creator<MyArtist>() {
        @Override
        public MyArtist createFromParcel(Parcel in) {
            return new MyArtist(in);
        }

        @Override
        public MyArtist[] newArray(int size) {
            return new MyArtist[size];
        }
    };
}

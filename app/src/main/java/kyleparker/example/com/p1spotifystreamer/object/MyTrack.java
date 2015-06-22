package kyleparker.example.com.p1spotifystreamer.object;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Track;

/**
 * Custom {@link Track} object used to implement Parcelable
 *
 * Created by kyleparker on 6/18/2015.
 */
public class MyTrack extends Track implements Parcelable{

    public MyTrack() { }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        try {
            parcel.writeString(id);
            parcel.writeString(name);
            parcel.writeString(album.name);

            parcel.writeList(artists);
            // TODO: Determine why this throws an error when on the Artist Track list and hit the home button
            // unable to marshal value kaaes.spotify.webapi.android.models.Image
            // Does the image object also need to be parcelable?
            parcel.writeList(album.images);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private MyTrack(Parcel source) {
        id = source.readString();
        name = source.readString();
        album.name = source.readString();

        ClassLoader classLoader = getClass().getClassLoader();

        artists = source.readParcelable(classLoader);
        album.images = source.readParcelable(classLoader);
    }

    public static final Creator<MyTrack> CREATOR = new Creator<MyTrack>() {
        @Override
        public MyTrack createFromParcel(Parcel in) {
            return new MyTrack(in);
        }

        @Override
        public MyTrack[] newArray(int size) {
            return new MyTrack[size];
        }
    };
}

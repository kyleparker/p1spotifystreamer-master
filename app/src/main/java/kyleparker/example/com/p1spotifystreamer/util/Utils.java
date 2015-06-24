package kyleparker.example.com.p1spotifystreamer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Various utilities for the project
 *
 * Created by kyleparker on 6/23/2015.
 */
public class Utils {

    /**
     * Check the connected state of the device
     *
     * @param context
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mgr.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected() && networkInfo.getState() == NetworkInfo.State.CONNECTED;

    }
}

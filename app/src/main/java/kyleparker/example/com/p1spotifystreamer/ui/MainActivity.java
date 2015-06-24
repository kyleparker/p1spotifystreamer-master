package kyleparker.example.com.p1spotifystreamer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import kyleparker.example.com.p1spotifystreamer.R;
import kyleparker.example.com.p1spotifystreamer.ui.fragment.ArtistListFragment;
import kyleparker.example.com.p1spotifystreamer.util.Constants;

// DONE: Convert to fragments and provide a tablet layout
// TODO: Add activity transition animations
// TODO: Test on a tablet - structure should be in place, just need to test and tweak

/**
 * Main activity for Project 1: Spotify Streamer app. This activity provides both phone and tablet layouts.
 *
 * Created by kyleparker on 6/15/2015.
 */
public class MainActivity extends BaseActivity implements
        ArtistListFragment.Callbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Toast.makeText(mActivity, R.string.toast_not_implemented, Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Setup the toolbar and add the title for the activity
     */
    private void setupToolbar() {
        final Toolbar toolbar = getActionBarToolbar();
        toolbar.setBackgroundColor(mActivity.getResources().getColor(R.color.theme_primary_dark));
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                toolbar.setTitle(mActivity.getString(R.string.app_name));
            }
        });
    }

    /**
     * Callback method from {@link ArtistListFragment.Callbacks} indicating that the item with the given
     * ID was selected.
     */
    @Override
    public void onItemSelected(String id, String artistName, String imageUrl) {
        // In single-pane mode, simply start the detail activity for the selected item ID.
        Intent detailIntent = new Intent(this, ArtistTrackActivity.class);
        detailIntent.putExtra(Constants.EXTRA_ARTIST_ID, id);
        detailIntent.putExtra(Constants.EXTRA_IMAGE_URL, imageUrl);
        detailIntent.putExtra(Constants.EXTRA_TITLE, artistName);
        startActivity(detailIntent);
    }
}

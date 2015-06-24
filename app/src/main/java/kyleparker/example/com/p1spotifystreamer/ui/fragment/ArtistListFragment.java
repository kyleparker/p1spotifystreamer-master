package kyleparker.example.com.p1spotifystreamer.ui.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kyleparker.example.com.p1spotifystreamer.R;
import kyleparker.example.com.p1spotifystreamer.object.MyArtist;
import kyleparker.example.com.p1spotifystreamer.util.Adapters;
import kyleparker.example.com.p1spotifystreamer.util.Constants;
import kyleparker.example.com.p1spotifystreamer.util.Utils;
import retrofit.RetrofitError;
import retrofit.client.Response;

// DONE: [Phone] UI contains a screen for searching for an artist and displaying a list of artist results
// DONE: Individual artist result layout contains - Artist Thumbnail , Artist name
// DONE: [Phone] UI places components in the same location and orientation as shown in the mockup
// DONE: App contains a search field that allows the user to enter in the name of an artist to search for
// DONE: When an artist name is entered, app displays list of artist results
// DONE: App displays a Toast if the artist name is not found (asks to refine search)
// DONE: When an artist is selected, app launches the "Top Tracks" View
// DONE: App implements Artist Search + GetTopTracks API Requests (using spotify wrapper)
// DONE: Save result list on device rotation - activity:configChanges, setRetainInstance or saveInstanceState
// DONE: Use SearchView instead of EditText
// DONE: Check for network connection before query
// DONE: Display loading spinner for search results
// TODO: Learn about "headless fragments" and setRetainInstance instead of Parcelable for saving data on rotation
/**
 * Fragment to display the search box and results of the user query
 *
 * Created by kyleparker on 6/16/2015.
 */
public class ArtistListFragment extends Fragment  {
    private Activity mActivity;
    private View mRootView;
    private View mHeader;
    private SearchView mSearchView;
    private RecyclerView mRecyclerView;
    private ProgressDialog mProgressDialog;

    private ArrayList<MyArtist> mArtistList;
    private Adapters.ArtistAdapter mAdapter;
    private GridLayoutManager mGridLayoutManager;

    private String mQuery;

    // The fragment's current callback object, which is notified of list item clicks.
    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this.getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_artist_list, container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupView();

        if (savedInstanceState != null) {
            mArtistList = savedInstanceState.getParcelableArrayList(Constants.KEY_ARTIST_ARRAY);
            if (!mArtistList.isEmpty()) {
                mAdapter.showHeader(mHeader, true);
                mAdapter.addAll(mArtistList);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mArtistList != null) {
            outState.putParcelableArrayList(Constants.KEY_ARTIST_ARRAY, mArtistList);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    /**
     * Handle the search function
     * <p/>
     * Setup the necessary views for the activity
     *
     * NOTE: In addition to reviewing the Android Developer documentation for RecyclerView, additional ideas and functionality
     * were pulled from this blog post: http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html.
     */
    private void setupView() {
        int artistsPerRow = mActivity.getResources().getInteger(R.integer.artists_per_row);

        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.artist_list);
        mRecyclerView.setHasFixedSize(true);
        // Define the gridlayout for the RecyclerView - column count will change based on rotation and device type
        mGridLayoutManager = new GridLayoutManager(mActivity, artistsPerRow);
        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mAdapter.isHeader(position) ? mGridLayoutManager.getSpanCount() : 1;
            }
        });
        mRecyclerView.setLayoutManager(mGridLayoutManager);

        // Add result header to the RecyclerView
        LayoutInflater inflater = mActivity.getLayoutInflater();
        mHeader = inflater.inflate(R.layout.header_search_result, mRecyclerView, false);

        // Setup adapter and artist list
        mArtistList = new ArrayList<>();
        mAdapter = new Adapters.ArtistAdapter(mActivity, mHeader, mArtistList);
        mAdapter.showHeader(mHeader, false);
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mRecyclerView.setAdapter(mAdapter);

        // Setup the input text to respond to the search action from the keyboard
        mSearchView = (SearchView) mRootView.findViewById(R.id.edit_search);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnTouchListener(mOnTouchListener);
        mSearchView.setOnQueryTextListener(mOnQueryTextListener);
        mSearchView.setQueryHint(getResources().getString(R.string.content_search));
    }

    /**
     * Define an query text listener for the SearchView. This will hide the keyboard and call the
     * Spotify service using the query. A {@link retrofit.Callback<ArtistsPager>} will handle updating
     * the UI and display the results. If no results are found, a toast message will be displayed.
     */
    private SearchView.OnQueryTextListener mOnQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            mQuery = mSearchView.getQuery().toString();
            mGridLayoutManager.smoothScrollToPosition(mRecyclerView, null, 0);

            if (Utils.isOnline(mActivity)) {
                if (!TextUtils.isEmpty(mQuery)) {
                    // Hide the keyboard after the user has submitted the search query
                    mSearchView.clearFocus();

                    mProgressDialog = ProgressDialog.show(mActivity, null, mActivity.getString(R.string.content_loading));
                    mProgressDialog.show();

                    // Spotify search using callback
                    SpotifyApi api = new SpotifyApi();
                    SpotifyService service = api.getService();

                    service.searchArtists(mQuery, mSpotifyCallback);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.toast_error_no_query), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getActivity(), getResources().getString(R.string.toast_error_not_online), Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };

    /**
     * Handle the {@link Adapters.ArtistAdapter.OnItemClickListener} event when the user selects an artist
     * from the search results
     */
    private Adapters.ArtistAdapter.OnItemClickListener mOnItemClickListener = new Adapters.ArtistAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            // Subtract one from the position to account for the header
            MyArtist artist = mAdapter.getItem(position - 1);

            if (artist != null) {
                // Notify the active callbacks interface (the activity, if the fragment is attached to one) that
                // an item has been selected.
                mCallbacks.onItemSelected(artist.id, artist.name, artist.getImageUrl());
            }
        }
    };

    /**
     * Handle the {@link View.OnTouchListener} event when a user touches the EditText search box. This will clear any
     * previously entered queries and set the box for a new search.
     */
    private View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            // Clear the input when the user touches on the box - prepare for a new search
            mSearchView.setQuery("", false);
            mSearchView.clearFocus();
            return false;
        }
    };

    /**
     * Callback for the Spotify searchArtist method. A successful search will load the adapter and display the results.
     */
    private retrofit.Callback<ArtistsPager> mSpotifyCallback = new retrofit.Callback<ArtistsPager>() {
        @Override
        public void success(ArtistsPager artistsPager, Response response) {
            if (response.getStatus() == Constants.STATUS_OK) {
                if (artistsPager == null || artistsPager.artists == null || artistsPager.artists.total == 0) {
                    // In order display the toast must be run on the main UI thread
                    // The callback does not have access to the original view from this thread
                    showToast(R.string.toast_no_results, true);
                    return;
                }

                // TODO: Determine how to retrieve the next set of results - iterate through the limit/total
//                Map<String, Object> options = new HashMap<>();
//                options.put(SpotifyService.OFFSET, 0);
//                options.put(SpotifyService.LIMIT, 10);

                mArtistList = new ArrayList<>();
                List<Artist> artistList = artistsPager.artists.items;

                for (Artist artist : artistList) {
                    MyArtist myArtist = new MyArtist();

                    myArtist.id = artist.id;
                    myArtist.name = artist.name;
                    if (artist.images != null && artist.images.size() > 0) {
                        myArtist.setImageUrl(artist.images.get(0).url);
                    }

                    mArtistList.add(myArtist);
                }

                // In order to update the adapter and the RecyclerView, the addAll method must be run on the main UI thread
                // The callback does not have access to the original view from this thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.showHeader(mHeader, true);
                        mAdapter.addAll(mArtistList);
                        mProgressDialog.dismiss();
                    }
                });
            } else {
                showToast(R.string.toast_error_results, true);
            }
        }

        @Override
        public void failure(RetrofitError error) {
            if (error.getKind() == RetrofitError.Kind.NETWORK) {
                showToast(R.string.toast_error_results, false);
            } else {
                showToast(R.string.toast_error_results, true);
            }
        }

        public void showToast(final int resId, final boolean includeQuery) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.dismiss();
                    Toast.makeText(mActivity, includeQuery ?
                            mActivity.getString(resId, mQuery) : mActivity.getString(resId), Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    /**
     * A callback interface that all activities containing this fragment must implement. This mechanism allows activities
     * to be notified of item selections.
     */
    public interface Callbacks {
        // Callback for when an item has been selected.
        void onItemSelected(String id, String artistName, String imageUrl);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does nothing. Used only when this fragment is not
     * attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id, String artistName, String imageUrl) { }
    };
}

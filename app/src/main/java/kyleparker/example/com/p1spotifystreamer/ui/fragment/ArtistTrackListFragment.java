package kyleparker.example.com.p1spotifystreamer.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import kyleparker.example.com.p1spotifystreamer.R;
import kyleparker.example.com.p1spotifystreamer.object.MyTrack;
import kyleparker.example.com.p1spotifystreamer.ui.BaseActivity;
import kyleparker.example.com.p1spotifystreamer.util.Adapters;
import kyleparker.example.com.p1spotifystreamer.util.Constants;
import retrofit.RetrofitError;
import retrofit.client.Response;

// DONE: [Phone] UI contains a screen for displaying the top tracks for a selected artist
// DONE: Individual track layout contains - Album art thumbnail, track name, album name
// DONE: [Phone] UI places components in the same location and orientation as shown in the mockup
// DONE: Switch this to a listview in order to use the parallax scrolling with floating toolbar?
// DONE: App displays a list of top tracks
// DONE: App implements Artist Search + GetTopTracks API Requests (using spotify wrapper)
// DONE: Handle device rotation - when the list is partially scrolled and orientation changes, the header doesn't reset
// DONE: App stores the most recent top tracks query results and their respective metadata (track name, artist name, album name)
// locally in list. The queried results are retained on rotation.
// TODO: Display a message if no tracks found for artist
/**
 * Fragment to display the top 10 tracks for a selected artist
 *
 * The fragment implements parallax scrolling with the artist image and toolbar. A library from
 * https://github.com/ksoichiro/Android-ObservableScrollView was used to implement this functionality.
 *
 * Created by kyleparker on 6/17/2015.
 */
public class ArtistTrackListFragment extends Fragment implements ObservableScrollViewCallbacks {
    private Activity mActivity;
    private View mRootView;

    private ArrayList<MyTrack> mTrackList;
    private Adapters.ArtistTrackAdapter mAdapter;

    private View mHeader;
    private View mHeaderBar;
    private View mListBackgroundView;
    private View mHeaderBackground;
    private ImageView mImage;

    private int mActionBarSize;
    private int mFlexibleSpaceImageHeight;
    private int mPrevScrollY;
    private boolean mGapIsChanging;
    private boolean mGapHidden;
    private boolean mReady;
    private String mArtistId;
    private String mArtistName;
    private String mImageUrl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this.getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_artist_track_list, container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        reloadFromArguments(getArguments());
        setupView();

        if (savedInstanceState != null) {
            mTrackList = savedInstanceState.getParcelableArrayList(Constants.KEY_TRACK_ARRAY);
            mAdapter.addAll(mTrackList);
        } else {
            getArtistTrackList();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!mTrackList.isEmpty()) {
            outState.putParcelableArrayList(Constants.KEY_TRACK_ARRAY, mTrackList);
        }
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        updateViews(scrollY, true);
    }

    @Override
    public void onDownMotionEvent() {
    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
    }

    /**
     * As the user scrolls, animate the background header
     *
     * @param shouldShowGap
     * @param animated
     */
    private void changeHeaderBackgroundHeightAnimated(boolean shouldShowGap, boolean animated) {
        if (mGapIsChanging) {
            return;
        }
        final int heightOnGapShown = mHeaderBar.getHeight();
        final int heightOnGapHidden = mHeaderBar.getHeight() + mActionBarSize;
        final float from = mHeaderBackground.getLayoutParams().height;
        final float to;
        if (shouldShowGap) {
            if (!mGapHidden) {
                // Already shown
                return;
            }
            to = heightOnGapShown;
        } else {
            if (mGapHidden) {
                // Already hidden
                return;
            }
            to = heightOnGapHidden;
        }
        if (animated) {
            ViewPropertyAnimator.animate(mHeaderBackground).cancel();
            ValueAnimator a = ValueAnimator.ofFloat(from, to);
            a.setDuration(100);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float height = (float) animation.getAnimatedValue();
                    changeHeaderBackgroundHeight(height, to, heightOnGapHidden);
                }
            });
            a.start();
        } else {
            changeHeaderBackgroundHeight(to, to, heightOnGapHidden);
        }
    }

    /**
     * As the user scrolls, change the background header height
     *
     * @param height
     * @param to
     * @param heightOnGapHidden
     */
    private void changeHeaderBackgroundHeight(float height, float to, float heightOnGapHidden) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mHeaderBackground.getLayoutParams();
        lp.height = (int) height;
        lp.topMargin = (int) (mHeaderBar.getHeight() - height);
        mHeaderBackground.requestLayout();
        mGapIsChanging = (height != to);
        if (!mGapIsChanging) {
            mGapHidden = (height == heightOnGapHidden);
        }
    }

    /**
     * Create an {@link ObservableRecyclerView} to enable the parallax effect with the RecyclerView and the background
     * artist image
     *
     * @return
     */
    private ObservableRecyclerView createScrollable() {
        ObservableRecyclerView recyclerView = (ObservableRecyclerView) mRootView.findViewById(R.id.artist_track_list);
        recyclerView.setScrollViewCallbacks(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        recyclerView.setHasFixedSize(false);

        View headerView = new View(mActivity);
        headerView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, mFlexibleSpaceImageHeight));
        headerView.setMinimumHeight(mFlexibleSpaceImageHeight);
        // This is required to disable header's list selector effect
        headerView.setClickable(false);

        mTrackList = new ArrayList<>();
        mAdapter = new Adapters.ArtistTrackAdapter(mActivity, mTrackList, headerView);
        recyclerView.setAdapter(mAdapter);

        return recyclerView;
    }

    /**
     * Get the height of the toolbar - to be used during the scrolling and parallax effect
     *
     * @return
     */
    private int getActionBarSize() {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = mActivity.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

    /**
     * Retrieve the Top 10 tracks for the artist, based on the artist selected on the previous screen
     *
     * TODO: Create a setting to allow the user to select the country - initially, default to US
     */
    private void getArtistTrackList() {
        // Spotify search using callback
        SpotifyApi api = new SpotifyApi();
        SpotifyService service = api.getService();

        Map<String, Object> options = new HashMap<>();
        options.put(SpotifyService.COUNTRY, Constants.US_COUNTRY_ID);
        options.put(SpotifyService.OFFSET, 0);
        options.put(SpotifyService.LIMIT, 10);

        service.getArtistTopTrack(mArtistId, options, mSpotifyCallback);
    }

    /**
     * Convert the fragment arguments to intents for use during the activity lifecycle
     */
    private void reloadFromArguments(Bundle arguments) {
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);

        Bundle extras = intent.getExtras();
        if (extras != null) {
            mArtistId = extras.getString(Constants.EXTRA_ARTIST_ID);
            mArtistName = extras.getString(Constants.EXTRA_TITLE);
            mImageUrl = extras.getString(Constants.EXTRA_IMAGE_URL);
        }
    }

    /**
     * Setup the various views for the fragment - including those needed for the parallax effect
     */
    private void setupView() {
        // Even when the top gap has began to change, header bar still can move within mIntersectionHeight.
        mFlexibleSpaceImageHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.flexible_space_image_height);
        mActionBarSize = getActionBarSize();

        mImage = (ImageView) mRootView.findViewById(R.id.image);
        mHeader = mRootView.findViewById(R.id.header);
        mHeaderBar = mRootView.findViewById(R.id.header_bar);
        mHeaderBackground = mRootView.findViewById(R.id.header_background);
        mListBackgroundView = mRootView.findViewById(R.id.list_background);

        TextView subtitleView = (TextView) mRootView.findViewById(R.id.subtitle);
        subtitleView.setText(mArtistName);

        final ObservableRecyclerView scrollable = createScrollable();
        ScrollUtils.addOnGlobalLayoutListener(scrollable, new Runnable() {
            @Override
            public void run() {
                mReady = true;
                updateViews(scrollable.getCurrentScrollY(), false);
            }
        });

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette palette) {
                        mImage.setImageBitmap(bitmap);
                        Palette.Swatch darkSwatch = palette.getDarkVibrantSwatch() == null ?
                                palette.getDarkMutedSwatch() : palette.getDarkVibrantSwatch();

                        if (darkSwatch != null) {
                            mHeaderBar.setBackgroundColor(darkSwatch.getRgb());
                            mHeaderBackground.setBackgroundColor(darkSwatch.getRgb());

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                Window window = mActivity.getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                                window.setStatusBarColor(darkSwatch.getRgb());
                            }
                        }
                    }
                });
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) { }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) { }
        };

        if (!TextUtils.isEmpty(mImageUrl)) {
            Picasso.with(mActivity)
                    .load(mImageUrl)
                    .resize(300, 300)
                    .centerCrop()
                    .into(target);
        } else {
            mImage.setImageResource(R.drawable.ic_placeholder_artist);
        }
    }

    private void updateViews(int scrollY, boolean animated) {
        // If it's ListView, onScrollChanged is called before ListView is laid out (onGlobalLayout).
        // This causes weird animation when onRestoreInstanceState occurred,
        // so we check if it's laid out already.
        if (!mReady) {
            return;
        }

        // Translate image
        ViewHelper.setTranslationY(mImage, -scrollY / 2);

        // Translate header
        float headerTranslationY = ScrollUtils.getFloat(-scrollY + mFlexibleSpaceImageHeight - mHeaderBar.getHeight(), 0,
                Float.MAX_VALUE);
        ViewHelper.setTranslationY(mHeader, headerTranslationY);

        // Show/hide gap
        final int headerHeight = mHeaderBar.getHeight();
        boolean scrollUp = mPrevScrollY < scrollY;
        if (scrollUp) {
            if (mFlexibleSpaceImageHeight - headerHeight - mActionBarSize <= scrollY) {
                changeHeaderBackgroundHeightAnimated(false, animated);
            }
        } else {
            if (scrollY <= mFlexibleSpaceImageHeight - headerHeight - mActionBarSize) {
                changeHeaderBackgroundHeightAnimated(true, animated);
            }
        }

        mPrevScrollY = scrollY;

        // Translate list background
        ViewHelper.setTranslationY(mListBackgroundView, ViewHelper.getTranslationY(mHeader));
    }

    /**
     * Callback for the Spotify searchArtist method. A successful search will load the adapter and display the results.
     */
    private retrofit.Callback<Tracks> mSpotifyCallback = new retrofit.Callback<Tracks>() {
        @Override
        public void success(Tracks result, Response response) {
            if (response.getStatus() == Constants.STATUS_OK) {
                if (result == null || result.tracks.isEmpty()) {
                    // In order display the toast must be run on the main UI thread
                    // The callback does not have access to the original view from this thread
                    showToast(R.string.toast_no_results);
                    return;
                }

                mTrackList = new ArrayList<>();
                List<Track> trackList = result.tracks;

                for (Track track : trackList) {
                    MyTrack myTrack = new MyTrack();
                    myTrack.album = new AlbumSimple();

                    myTrack.id = track.id;
                    myTrack.name = track.name;
                    myTrack.album.name = track.album.name;
                    myTrack.album.images = track.album.images;

                    mTrackList.add(myTrack);
                }

                // In order to update the adapter and the recyclerview, the addAll method must be run on the main UI thread
                // The callback does not have access to the original view from this thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.addAll(mTrackList);
                    }
                });
            } else {
                showToast(R.string.toast_error_results);
            }
        }

        @Override
        public void failure(RetrofitError error) {
            showToast(R.string.toast_error_results);
        }

        public void showToast(final int resId) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, mActivity.getString(resId, mArtistName), Toast.LENGTH_LONG).show();
                }
            });
        }
    };
}

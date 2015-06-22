package kyleparker.example.com.p1spotifystreamer.util;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.poliveira.parallaxrecycleradapter.ParallaxRecyclerAdapter;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kyleparker.example.com.p1spotifystreamer.R;
import kyleparker.example.com.p1spotifystreamer.object.MyTrack;

/**
 * Adapter class for the RecyclerView
 * <p/>
 * Based on the code sample provided by Google - https://developer.android.com/samples/RecyclerView/index.html
 * Headers were added based on this blog post - http://blog.sqisland.com/2014/12/recyclerview-grid-with-header.html
 * Example on how to add click listener - https://github.com/VenomVendor/RecyclerView
 * <p/>
 * Created by kyleparker on 6/15/2015.
 */
public class Adapters {
    public static class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {
        private Context context;
        private List<Artist> items;
        private View header;
        private OnItemClickListener itemClickListener;

        private static final int ITEM_VIEW_TYPE_HEADER = 0;
        private static final int ITEM_VIEW_TYPE_ITEM = 1;

        public ArtistAdapter(Context context, View header, List<Artist> items) {
            this.context = context;
            this.header = header;
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == ITEM_VIEW_TYPE_HEADER) {
                return new ViewHolder(header);
            }

            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_artist, viewGroup, false);

            return new ViewHolder(v);
        }

        public void showHeader(View view, boolean visible) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            if (isHeader(position)) {
                return;
            }

            // Subtract 1 for the header
            Artist item = items.get(position - 1);

            if (item != null) {
                viewHolder.getArtistName().setText(item.name);
                if (!item.images.isEmpty()) {
                    Picasso.with(context)
                            .load(item.images.get(0).url)
                            .resize(300, 300)
                            .centerCrop()
                            .into(viewHolder.getArtistThumb(), getLoaderCallback(viewHolder.getProgress()));
                } else {
                    viewHolder.getArtistThumb().setImageResource(R.drawable.ic_placeholder_artist);
                    viewHolder.getProgress().setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount() {
            // Add 1 to retrieve the total number of items, including the header
            return items.size() + 1;
        }

        public Artist getItem(int position) {
            return items.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return isHeader(position) ? ITEM_VIEW_TYPE_HEADER : ITEM_VIEW_TYPE_ITEM;
        }

        public boolean isHeader(int position) {
            return position == 0;
        }

        public void addAll(List<Artist> artists) {
            items.clear();
            items.addAll(artists);
            notifyDataSetChanged();
        }

        public void setOnItemClickListener(final OnItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        public interface OnItemClickListener {
            void onItemClick(View view, int position);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private TextView artistName;
            private ImageView artistThumb;
            private ProgressBar progress;

            public ViewHolder(View base) {
                super(base);

                artistName = (TextView) base.findViewById(R.id.artist_name);
                artistThumb = (ImageView) base.findViewById(R.id.artist_thumb);
                progress = (ProgressBar) base.findViewById(R.id.progress);

                base.setOnClickListener(this);
            }

            public TextView getArtistName() {
                return artistName;
            }

            public ImageView getArtistThumb() {
                return artistThumb;
            }

            public ProgressBar getProgress() {
                return progress;
            }

            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(v, getPosition());
                }
            }
        }

        private Callback getLoaderCallback(final ProgressBar progressBar) {
            return new Callback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    progressBar.setVisibility(View.GONE);
                }
            };
        }
    }

    public static class ArtistTrackAdapter extends RecyclerView.Adapter<ArtistTrackAdapter.ViewHolder> {
        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;

        private Context mContext;
        private List<MyTrack> mItems;
        private View mHeader;
//        private static OnItemClickListener itemClickListener;

        public ArtistTrackAdapter(Context context, List<MyTrack> items, View header) {
            mContext = context;
            mItems = items;
            mHeader = header;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            if (viewType == VIEW_TYPE_HEADER) {
                return new ViewHolder(mHeader);
            }

            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_artist_track, viewGroup, false);

            return new ViewHolder(v);
        }

        @Override
        public int getItemViewType(int position) {
            return (position == 0) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
        }

        public boolean isHeader(int position) {
            return position == 0;
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            if (isHeader(position)) {
                return;
            }

            MyTrack item = mItems.get(position);

            if (item != null) {
                viewHolder.getAlbumName().setText(item.album.name);
                viewHolder.getTrackName().setText(item.name);
                if (!item.album.images.isEmpty()) {
                    Picasso.with(mContext)
                            .load(item.album.images.get(0).url)
                            .resize(300, 300)
                            .centerCrop()
                            .into(viewHolder.getAlbumThumb(), getLoaderCallback(viewHolder.getProgress()));
                } else {
                    viewHolder.getAlbumThumb().setImageResource(R.drawable.ic_placeholder_artist);
                    viewHolder.getProgress().setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemCount () {
            return mItems.size();
        }

//        public MyTrack getItem(int position) {
//            return mItems.get(position);
//        }

        public void addAll(List<MyTrack> tracks) {
            mItems.clear();
            mItems.addAll(tracks);
            notifyDataSetChanged();
        }

//        public void setOnItemClickListener(final OnItemClickListener itemClickListener) {
//            ArtistTrackAdapter.itemClickListener = itemClickListener;
//        }

//        public interface OnItemClickListener {
//            void onItemClick(View view, int position);
//        }

        public class ViewHolder extends RecyclerView.ViewHolder { //implements View.OnClickListener {
            private TextView albumName;
            private TextView trackName;
            private ImageView albumThumb;
            private ProgressBar progress;

            public ViewHolder(View base) {
                super(base);

                albumName = (TextView) base.findViewById(R.id.album_name);
                trackName = (TextView) base.findViewById(R.id.track_name);
                albumThumb = (ImageView) base.findViewById(R.id.album_thumb);
                progress = (ProgressBar) base.findViewById(R.id.progress);

//                base.setOnClickListener(this);
            }

            public TextView getAlbumName() {
                return albumName;
            }

            public TextView getTrackName() {
                return trackName;
            }

            public ImageView getAlbumThumb() {
                return albumThumb;
            }

            public ProgressBar getProgress() {
                return progress;
            }

//            @Override
//            public void onClick(View v) {
//                if (itemClickListener != null) {
//                    itemClickListener.onItemClick(v, getPosition());
//                }
//            }
        }

        private Callback getLoaderCallback(final ProgressBar progressBar) {
            return new Callback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    progressBar.setVisibility(View.GONE);
                }
            };
        }
    }
}

package com.iven.musicplayergo;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.iven.musicplayergo.adapters.AlbumsAdapter;
import com.iven.musicplayergo.adapters.ArtistsAdapter;
import com.iven.musicplayergo.adapters.ColorsAdapter;
import com.iven.musicplayergo.adapters.SongsAdapter;
import com.iven.musicplayergo.loaders.ArtistProvider;
import com.iven.musicplayergo.loaders.SongProvider;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;
import com.iven.musicplayergo.models.Song;
import com.iven.musicplayergo.playback.EqualizerUtils;
import com.iven.musicplayergo.playback.MusicNotificationManager;
import com.iven.musicplayergo.playback.MusicService;
import com.iven.musicplayergo.playback.PlaybackInfoListener;
import com.iven.musicplayergo.playback.PlayerAdapter;
import com.iven.musicplayergo.roundedfastscroller.RoundedFastScrollRecyclerView;

import java.util.Collections;
import java.util.List;

public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<List<Artist>>, SongsAdapter.SongSelectedListener, ColorsAdapter.AccentChangedListener, AlbumsAdapter.AlbumSelectedListener, ArtistsAdapter.ArtistSelectedListener {

    private final int ANIMATION_DURATION = 500;
    private LinearLayoutManager mArtistsLayoutManager, mAlbumsLayoutManager, mSongsLayoutManager;
    private int mAccent;
    private boolean sThemeInverted;
    private RoundedFastScrollRecyclerView mArtistsRecyclerView;
    private RecyclerView mAlbumsRecyclerView, mSongsRecyclerView;
    private AlbumsAdapter mAlbumsAdapter;
    private SongsAdapter mSongsAdapter;
    private TextView mPlayingAlbum, mPlayingSong, mDuration, mSongPosition, mArtistAlbumCount, mSelectedAlbum;
    private SeekBar mSeekBarAudio;
    private LinearLayout mControlsContainer;
    private View mSettingsView, mPlayerInfoView, mArtistDetails;
    private ImageView mPlayPauseButton, mResetButton, mExpandImage;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    private List<Artist> mArtists;
    private String mSelectedArtist;
    private boolean sExpandArtistDiscography = false;
    private boolean sPlayerInfoLongPressed = false;
    private boolean sArtistDiscographyExpanded = false;
    private MusicService mMusicService;
    private PlaybackListener mPlaybackListener;
    private List<Song> mSelectedArtistSongs;
    private MusicNotificationManager mMusicNotificationManager;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            mMusicService = ((MusicService.LocalBinder) iBinder).getInstance();
            mPlayerAdapter = mMusicService.getMediaPlayerHolder();
            mMusicNotificationManager = mMusicService.getMusicNotificationManager();
            mMusicNotificationManager.setAccentColor(mAccent);

            if (mPlaybackListener == null) {
                mPlaybackListener = new PlaybackListener();
                mPlayerAdapter.setPlaybackInfoListener(mPlaybackListener);
            }
            checkReadStoragePermissions();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mMusicService = null;
        }
    };
    private boolean mIsBound;
    private Parcelable mSavedArtistRecyclerLayoutState;
    private Parcelable mSavedAlbumsRecyclerLayoutState;
    private Parcelable mSavedSongRecyclerLayoutState;

    @Override
    public void onPause() {
        super.onPause();
        if (mArtistsLayoutManager != null && mAlbumsLayoutManager != null && mSongsLayoutManager != null) {
            mSavedArtistRecyclerLayoutState = mArtistsLayoutManager.onSaveInstanceState();
            mSavedAlbumsRecyclerLayoutState = mAlbumsLayoutManager.onSaveInstanceState();
            mSavedSongRecyclerLayoutState = mSongsLayoutManager.onSaveInstanceState();
        }
        if (mPlayerAdapter != null && mPlayerAdapter.isMediaPlayer()) {
            mPlayerAdapter.onPauseActivity();
        }
        if (mSettingsView.getVisibility() == View.VISIBLE) {
            revealView(mSettingsView, mControlsContainer, true, false);
        }
    }

    @Override
    public void onAccentChanged(final int color) {
        mMusicNotificationManager.setAccentColor(color);
        if (mPlayerAdapter.isMediaPlayer()) {
            mMusicNotificationManager.getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager.createNotification());
        }
        Utils.setThemeAccent(this, color);
    }

    @Override
    public void onBackPressed() {
        //if the reveal view is expanded collapse it
        if (sArtistDiscographyExpanded) {
            revealView(mArtistDetails, mArtistsRecyclerView, false, false);
        } else if (mSettingsView.getVisibility() == View.VISIBLE) {
            closeSettings(mSettingsView);
        } else {
            super.onBackPressed();
        }
    }

    private void checkReadStoragePermissions() {
        if (Utils.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                showPermissionRationale();
            } else {
                onPermissionGranted();
            }
        } else {
            onPermissionGranted();
        }
    }

    @TargetApi(23)
    private void showPermissionRationale() {
        final AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.setIcon(R.drawable.ic_folder);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.perm_rationale));
        builder.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final int READ_FILES_CODE = 2588;
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                                , READ_FILES_CODE);
                    }
                });
        builder.setCanceledOnTouchOutside(false);
        try {
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            showPermissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sThemeInverted = Utils.isThemeInverted(this);
        mAccent = Utils.getAccent(this);

        Utils.setTheme(this, sThemeInverted, mAccent);

        setContentView(R.layout.main_activity);

        getViews();

        initializeSettings();

        setupViewParams();

        initializeSeekBar();

        doBindService();
    }

    private void getViews() {

        mControlsContainer = findViewById(R.id.controls_container);

        mArtistDetails = findViewById(R.id.artist_details);
        mPlayerInfoView = findViewById(R.id.player_info);
        mPlayingSong = findViewById(R.id.playing_song);
        mPlayingAlbum = findViewById(R.id.playing_album);
        mExpandImage = findViewById(R.id.expand);
        setupPlayerInfoTouchBehaviour();

        mPlayPauseButton = findViewById(R.id.play_pause);

        mResetButton = findViewById(R.id.replay);
        mSeekBarAudio = findViewById(R.id.seekTo);

        mDuration = findViewById(R.id.duration);
        mSongPosition = findViewById(R.id.song_position);
        mArtistAlbumCount = findViewById(R.id.artist_album_count);
        mSelectedAlbum = findViewById(R.id.selected_disc);

        mArtistsRecyclerView = findViewById(R.id.artists_rv);

        mArtistsRecyclerView.setTrackColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, mAccent), sThemeInverted ? 15 : 30));

        mAlbumsRecyclerView = findViewById(R.id.albums_rv);
        mSongsRecyclerView = findViewById(R.id.songs_rv);

        mSettingsView = findViewById(R.id.settings_view);
    }

    //https://stackoverflow.com/questions/6183874/android-detect-end-of-long-press
    private void setupPlayerInfoTouchBehaviour() {
        mPlayerInfoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!sPlayerInfoLongPressed) {
                    mPlayingSong.setSelected(true);
                    mPlayingAlbum.setSelected(true);
                    sPlayerInfoLongPressed = true;
                }
                return true;
            }
        });
        mPlayerInfoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (sPlayerInfoLongPressed) {
                        mPlayingSong.setSelected(false);
                        mPlayingAlbum.setSelected(false);
                        sPlayerInfoLongPressed = false;
                    }
                }
                return false;
            }
        });
    }

    private void setupViewParams() {
        final ViewTreeObserver observer = mControlsContainer.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int controlsContainerHeight = mControlsContainer.getHeight();

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mArtistsRecyclerView.getLayoutParams();
                layoutParams.topMargin = mPlayerInfoView.getHeight();
                mArtistsRecyclerView.setLayoutParams(layoutParams);
                mSettingsView.setMinimumHeight(controlsContainerHeight);
                mControlsContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void initializeSettings() {
        if (!EqualizerUtils.hasEqualizer(this)) {
            final ImageView eqButton = findViewById(R.id.eq);
            eqButton.getDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
        initializeColorsSettings();
    }

    public void showSettings(View v) {
        revealView(mSettingsView, mControlsContainer, true, true);
    }

    public void closeSettings(View v) {
        revealView(mSettingsView, mControlsContainer, true, false);
    }

    public void shuffleSongs(View v) {
        final List<Song> songs = sArtistDiscographyExpanded ? mSelectedArtistSongs : SongProvider.getAllDeviceSongs();
        Collections.shuffle(songs);
        onSongSelected(songs.get(0), songs);
    }

    private void setArtistsRecyclerView(@NonNull final List<Artist> data) {
        mArtistsLayoutManager = new LinearLayoutManager(this);
        mArtistsRecyclerView.setLayoutManager(mArtistsLayoutManager);
        final ArtistsAdapter artistsAdapter = new ArtistsAdapter(this, data);
        mArtistsRecyclerView.setAdapter(artistsAdapter);
    }

    private void initializeSeekBar() {
        mSeekBarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    final int currentPositionColor = mSongPosition.getCurrentTextColor();
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if (fromUser) {
                            userSelectedPosition = progress;
                            mSongPosition.setTextColor(ContextCompat.getColor(MainActivity.this, mAccent));
                        }
                        mSongPosition.setText(Song.formatDuration(progress));
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                        if (mUserIsSeeking) {
                            mSongPosition.setTextColor(currentPositionColor);
                        }
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    public void reset(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.reset();
            updateResetStatus(false);
        }
    }

    public void skipPrev(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.instantReset();
        }
    }

    public void resumeOrPause(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.resumeOrPause();
        }
    }

    public void skipNext(View v) {
        if (checkIsPlayer()) {
            mPlayerAdapter.skip(true);
        }
    }

    public void openEqualizer(View v) {
        if (EqualizerUtils.hasEqualizer(this)) {
            if (checkIsPlayer()) {
                mPlayerAdapter.openEqualizer(MainActivity.this);
            }
        } else {
            Toast.makeText(this, getString(R.string.no_eq), Toast.LENGTH_SHORT).show();
        }
    }

    public void openGitPage(View v) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/enricocid/Music-Player-GO")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean checkIsPlayer() {

        boolean isPlayer = mPlayerAdapter.isMediaPlayer();
        if (!isPlayer) {
            EqualizerUtils.notifyNoSessionId(this);
        }
        return isPlayer;
    }

    public void switchTheme(View v) {
        //avoid service killing when the player is in paused state
        if (mPlayerAdapter != null && mPlayerAdapter.getState() == PlaybackInfoListener.State.PAUSED) {
            mMusicService.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().createNotification());
            mMusicService.setRestoredFromPause(true);
        }
        Utils.invertTheme(this);
    }

    private void initializeColorsSettings() {

        final RecyclerView colorsRecyclerView = mSettingsView.findViewById(R.id.colors_rv);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        colorsRecyclerView.setLayoutManager(linearLayoutManager);
        colorsRecyclerView.setAdapter(new ColorsAdapter(this, mAccent));
    }

    private void onPermissionGranted() {
        getSupportLoaderManager().initLoader(ArtistProvider.ARTISTS_LOADER, null, this);
    }

    private void updateResetStatus(boolean onPlaybackCompletion) {
        final int color = onPlaybackCompletion ? Color.BLACK : mPlayerAdapter.isReset() ? Color.WHITE : Color.BLACK;
        mResetButton.post(new Runnable() {
            @Override
            public void run() {
                mResetButton.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        });
    }

    private void updatePlayingStatus() {
        final int drawable = mPlayerAdapter.getState() != PlaybackInfoListener.State.PAUSED ? R.drawable.ic_pause : R.drawable.ic_play;
        mPlayPauseButton.post(new Runnable() {
            @Override
            public void run() {
                mPlayPauseButton.setImageResource(drawable);
            }
        });
    }

    private void updatePlayingInfo(boolean restore, boolean startPlay) {

        if (startPlay) {
            mPlayerAdapter.getMediaPlayer().start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mMusicService.startForeground(MusicNotificationManager.NOTIFICATION_ID, mMusicNotificationManager.createNotification());
                }
            }, 250);
        }

        final Song selectedSong = mPlayerAdapter.getCurrentSong();

        mSelectedArtist = selectedSong.artistName;
        final int duration = selectedSong.duration;
        mSeekBarAudio.setMax(duration);
        Utils.updateTextView(mDuration, Song.formatDuration(duration));

        final Spanned spanned = Utils.buildSpanned(getString(R.string.playing_song, mSelectedArtist, selectedSong.title));

        mPlayingSong.post(new Runnable() {
            @Override
            public void run() {
                mPlayingSong.setText(spanned);
            }
        });

        Utils.updateTextView(mPlayingAlbum, selectedSong.albumName);

        if (restore) {
            mSeekBarAudio.setProgress(mPlayerAdapter.getPlayerPosition());
            updatePlayingStatus();
            updateResetStatus(false);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //stop foreground if coming from pause state
                    if (mMusicService.isRestoredFromPause()) {
                        mMusicService.stopForeground(false);
                        mMusicService.getMusicNotificationManager().getNotificationManager().notify(MusicNotificationManager.NOTIFICATION_ID, mMusicService.getMusicNotificationManager().getNotificationBuilder().build());
                        mMusicService.setRestoredFromPause(false);
                    }
                }
            }, 250);
        }
    }

    private void restorePlayerStatus() {

        mSeekBarAudio.setEnabled(mPlayerAdapter.isMediaPlayer());

        //if we are playing and the activity was restarted
        //update the controls panel
        if (mPlayerAdapter != null && mPlayerAdapter.isMediaPlayer()) {

            mPlayerAdapter.onResumeActivity();
            updatePlayingInfo(true, false);
        }
    }

    private void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this,
                MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;

        final Intent startNotStickyIntent = new Intent(this, MusicService.class);
        startService(startNotStickyIntent);
    }

    private void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    @NonNull
    public Loader<List<Artist>> onCreateLoader(int id, Bundle args) {
        return new ArtistProvider.AsyncArtistLoader(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Artist>> loader, List<Artist> artists) {

        if (artists.isEmpty()) {

            Toast.makeText(this, getString(R.string.error_no_music), Toast.LENGTH_SHORT)
                    .show();
            finish();

        } else {

            mArtists = artists;
            setArtistsRecyclerView(mArtists);
            mSelectedArtist = mPlayerAdapter.getSelectedAlbum() != null ? mPlayerAdapter.getSelectedAlbum().getArtistName() : mArtists.get(0).getName();
            setArtistDetails(ArtistProvider.getArtist(mArtists, mSelectedArtist).albums);
            if (mSavedArtistRecyclerLayoutState != null && mSavedAlbumsRecyclerLayoutState != null && mSavedSongRecyclerLayoutState != null) {
                mArtistsLayoutManager.onRestoreInstanceState(mSavedArtistRecyclerLayoutState);
                mAlbumsLayoutManager.onRestoreInstanceState(mSavedAlbumsRecyclerLayoutState);
                mSongsLayoutManager.onRestoreInstanceState(mSavedSongRecyclerLayoutState);
            }
        }
    }

    private void setArtistDetails(List<Album> albums) {
        if (mAlbumsAdapter != null) {
            mAlbumsRecyclerView.scrollToPosition(0);
            //only notify recycler view of item changed if an adapter already exists
            mAlbumsAdapter.swapArtist(albums);
        } else {
            mAlbumsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            mAlbumsRecyclerView.setLayoutManager(mAlbumsLayoutManager);
            mAlbumsAdapter = new AlbumsAdapter(this, albums, mPlayerAdapter, ContextCompat.getColor(this, mAccent));
            mAlbumsRecyclerView.setAdapter(mAlbumsAdapter);
        }

        mSelectedArtistSongs = SongProvider.getAllArtistSongs(albums);
        Utils.updateTextView(mArtistAlbumCount, getString(R.string.albums, mSelectedArtist, albums.size()));

        if (sExpandArtistDiscography) {
            revealView(mArtistDetails, mArtistsRecyclerView, false, true);
            sExpandArtistDiscography = false;
        } else {
            restorePlayerStatus();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader loader) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlaybackListener = null;
        doUnbindService();
    }

    @Override
    public void onSongSelected(@NonNull final Song song, @NonNull final List<Song> songs) {

        if (mSettingsView.getVisibility() == View.VISIBLE) {
            revealView(mSettingsView, mControlsContainer, true, false);
        }
        if (!mSeekBarAudio.isEnabled()) {
            mSeekBarAudio.setEnabled(true);
        }
        mPlayerAdapter.setCurrentSong(song, songs);
        mPlayerAdapter.initMediaPlayer();
    }

    @Override
    public void onArtistSelected(@NonNull final String artist) {

        if (!mSelectedArtist.equals(artist)) {

            //make the panel expandable
            sExpandArtistDiscography = true;
            mPlayerAdapter.setSelectedAlbum(null);

            //load artist albums only if not already loaded
            mSelectedArtist = artist;
            setArtistDetails(ArtistProvider.getArtist(mArtists, mSelectedArtist).albums);

        } else {
            expandArtistDetails(mArtistDetails);
            //if already loaded expand the panel
            revealView(mArtistDetails, mArtistsRecyclerView, false, true);
        }
    }

    @Override
    public void onAlbumSelected(@NonNull final Album album) {
        Utils.updateTextView(mSelectedAlbum, album.getTitle());
        mPlayerAdapter.setSelectedAlbum(album);
        if (mSongsAdapter != null) {
            mSongsRecyclerView.scrollToPosition(0);
            mSongsAdapter.swapSongs(album);
        } else {
            mSongsLayoutManager = new LinearLayoutManager(this);
            mSongsRecyclerView.setLayoutManager(mSongsLayoutManager);
            mSongsAdapter = new SongsAdapter(this, album);
            mSongsRecyclerView.setAdapter(mSongsAdapter);
        }
    }

    public void expandArtistDetails(View v) {
        revealView(mArtistDetails, mArtistsRecyclerView, false, !sArtistDiscographyExpanded);
    }

    private void rotateExpandImage(final boolean expand) {

        final int ALPHA_DURATION = 250;
        final float PIVOT_VALUE = 0.5f;
        final int PIVOT_TYPE = RotateAnimation.RELATIVE_TO_SELF;
        final float from = expand ? 0.0f : 180.0f;
        final float to = expand ? 180.0f : 0.0f;

        final AnimationSet animSet = new AnimationSet(true);
        animSet.setInterpolator(new DecelerateInterpolator());
        animSet.setFillAfter(true);
        animSet.setFillEnabled(true);
        final RotateAnimation animRotate = new RotateAnimation(from, to, PIVOT_TYPE, PIVOT_VALUE, PIVOT_TYPE, PIVOT_VALUE);
        animRotate.setDuration(ANIMATION_DURATION);
        animRotate.setFillAfter(true);
        animRotate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (!expand) {
                    mExpandImage.setVisibility(View.VISIBLE);
                    mExpandImage.animate().alpha(1.0f).setDuration(ALPHA_DURATION).start();
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (expand) {
                    mExpandImage.animate().alpha(0.0f).setDuration(ALPHA_DURATION).start();
                    mExpandImage.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        animSet.addAnimation(animRotate);
        mExpandImage.startAnimation(animSet);
    }

    private void revealView(final View viewToReveal, final View viewToHide, final boolean isSettings, boolean show) {

        final int viewToRevealHeight = viewToReveal.getHeight();
        final int viewToRevealWidth = viewToReveal.getWidth();
        final int viewToRevealHalfWidth = viewToRevealWidth / 2;
        final int radius = (int) Math.hypot(viewToRevealWidth, viewToRevealHeight);
        final int fromY = isSettings ? viewToRevealHeight / 2 : viewToHide.getTop() / 2;

        if (show) {
            final Animator anim = ViewAnimationUtils.createCircularReveal(viewToReveal, viewToRevealHalfWidth, fromY, 0, radius);
            anim.setDuration(ANIMATION_DURATION);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    if (!isSettings) {
                        rotateExpandImage(true);
                    }
                    viewToReveal.setVisibility(View.VISIBLE);
                    viewToHide.setVisibility(View.INVISIBLE);
                    viewToReveal.setClickable(false);
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    if (!isSettings) {
                        sArtistDiscographyExpanded = true;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
            anim.start();

        } else {

            final Animator anim = ViewAnimationUtils.createCircularReveal(viewToReveal, viewToRevealHalfWidth, fromY, radius, 0);
            anim.setDuration(ANIMATION_DURATION);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {
                    if (!isSettings) {
                        rotateExpandImage(false);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    viewToReveal.setVisibility(View.INVISIBLE);
                    viewToHide.setVisibility(View.VISIBLE);
                    viewToReveal.setClickable(true);
                    if (!isSettings) {
                        sArtistDiscographyExpanded = false;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                }

                @Override
                public void onAnimationRepeat(Animator animator) {
                }
            });
            anim.start();
        }
    }

    class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekBarAudio.setProgress(position);
            }
        }

        @Override
        public void onStateChanged(@State int state) {

            updatePlayingStatus();
            if (mPlayerAdapter.getState() != State.RESUMED && mPlayerAdapter.getState() != State.PAUSED) {
                updatePlayingInfo(false, true);
            }
        }

        @Override
        public void onPlaybackCompleted() {
            updateResetStatus(true);
        }
    }
}

package taha.younis.musicappjava;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;
import taha.younis.musicappjava.adapters.RecyclerView_InterFace;
import taha.younis.musicappjava.adapters.SongAdapter;
import taha.younis.musicappjava.data.Song;

public class MainActivity extends AppCompatActivity implements RecyclerView_InterFace {

    SongAdapter songAdapter;
    List <Song> allSongs = new ArrayList <>();
    RecyclerView rvSongList;
    ExoPlayer player;

    ActivityResultLauncher <String> recordAudioPermissionLauncher; // to access in the song adapter
    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;
    ConstraintLayout playerView;
    TextView playerCloseBtn;
    // controls
    TextView songNameView, skipPreviousBtn, skipNextBtn, playPauseBtn, repeatModeBtn, playListBtn;
    TextView homeSongNameView, homeSkipPreviousBtn, homeSkipNextBtn, homePlayPauseBtn;
    // wrappers
    ConstraintLayout homeControlWrapper, headWrapper, artworkWrapper, seekbarWrapper, controlWrapper, audioVisualizerWrapper;

    CircleImageView artworkView;
    SeekBar seekBar;
    TextView progressView, durationView;
    BarVisualizer audioVisualizer;
    BlurImageView blurImageView;
    // statusBar and navigation color
    int defaultStatusColor;
    int repeatMode; // repeat all = 1, repeat one = 2, shuffle = 3
    // is activity bound?
    boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // save status color
        defaultStatusColor = getWindow().getStatusBarColor();
        // set the navigation color
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));

        bindViews();

//        runtimePermission();

        recordAudioPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted && player.isPlaying()) {
                activateAudioVisualizer();
            } else {
                userResponsesOnRecordAudioPerm();
            }
        });

//        player = new ExoPlayer.Builder(this).build();

//        playerControlMethods();

        // we are not going to do anything on the player unless we are bound to the player service, so let's bind the player service
        doBindService();

    }

    private void doBindService() {
        /* define a method the intent which is going to sent to onBind method and after onBind called
         then the ServiceBind intercept the player service instead the activity */
        Intent playerServiceIntent = new Intent(this, PlayerService.class);
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            // get the service instance
            PlayerService.ServiceBinder binder = (PlayerService.ServiceBinder) iBinder;
            player = binder.getPlayerService().player;
            isBound = true;
            // ready to show the songs
            runtimePermission();
            playerControlMethods();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void playerControlMethods() {
        // song name marquee
        homeSongNameView.setSelected(true);
        songNameView.setSelected(true);

        //exit the player view
        playerCloseBtn.setOnClickListener(v -> {
            exitPlayerView();
        });
        playListBtn.setOnClickListener(v -> {
            exitPlayerView();
        });
        //open player when home control clicked
        homeControlWrapper.setOnClickListener(v -> {
            showPlayerView();
        });

        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                assert mediaItem != null;
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);

                progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                seekBar.setProgress((int) player.getCurrentPosition());
                seekBar.setMax((int) player.getDuration());
                durationView.setText(getReadableTime((int) player.getDuration()));
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline, 0, 0, 0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);

                showCurrentArtwork();

                //update the progress position of the current playing song
                updatePlayerPositionProgress();

                artworkView.setAnimation(loadRotation());

                activateAudioVisualizer();

                updatePlayerColors();

                if (!player.isPlaying()) {
                    player.play();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if (playbackState == ExoPlayer.STATE_READY) {
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(player.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) player.getDuration()));
                    seekBar.setMax((int) player.getDuration());
                    seekBar.setProgress((int) player.getCurrentPosition());
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline, 0, 0, 0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);

                    showCurrentArtwork();

                    //update the progress position of the current playing song
                    updatePlayerPositionProgress();

                    artworkView.setAnimation(loadRotation());

                    activateAudioVisualizer();

                    updatePlayerColors();
                } else {
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline, 0, 0, 0);
                }
            }
        });

        // skip next or previous
        skipNextBtn.setOnClickListener(v -> {
            skipToNextSong();
        });
        homeSkipNextBtn.setOnClickListener(v -> {
            skipToNextSong();
        });
        skipPreviousBtn.setOnClickListener(v -> {
            skipToPreviousSong();
        });
        homeSkipPreviousBtn.setOnClickListener(v -> {
            skipToPreviousSong();
        });
        // play or pause song
        playPauseBtn.setOnClickListener(v -> {
            playOrPausePlayer();
        });
        homePlayPauseBtn.setOnClickListener(v -> {
            playOrPausePlayer();
        });

        //seek bar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player.getPlaybackState() == ExoPlayer.STATE_READY) {
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    player.seekTo(progressValue);
                }
            }
        });

        // repeat mode
        repeatModeBtn.setOnClickListener(v -> {
            if (repeatMode == 1) {
                // repeat one
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode = 2;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one, 0, 0, 0);
            } else if (repeatMode == 2) {
                // shuffle all
                player.setShuffleModeEnabled(true);
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode = 3;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle, 0, 0, 0);
            } else {
                // the songs are shuffling
                // repeat all
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                repeatMode = 1;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeate_all, 0, 0, 0);
            }
            updatePlayerColors();
        });
    }

    private void playOrPausePlayer() {
        if (player.isPlaying()) {
            player.pause();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline, 0, 0, 0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
            artworkView.clearAnimation();
        } else {
            player.play();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline, 0, 0, 0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
            artworkView.startAnimation(loadRotation());
        }

        updatePlayerColors();
    }

    private void skipToNextSong() {
        if (player.hasNextMediaItem()) {
            player.seekToNext();
        }
    }

    private void skipToPreviousSong() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious();
        }
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation =
                new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()) {
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    seekBar.setProgress((int) player.getCurrentPosition());
                }
                // repeat calling the method
                updatePlayerPositionProgress();
            }
        }, 1000);
    }

    private void showCurrentArtwork() {
        artworkView.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem().mediaMetadata.artworkUri));
        if (artworkView.getDrawable() == null)
            artworkView.setImageResource(R.drawable.img_music);
    }

    String getReadableTime(int duration) {
        String time;
        int hrs = duration / (1000 * 60 * 60);
        int min = (duration % (1000 * 60 * 60)) / (1000 * 60);
        int sec = (((duration % (1000 * 60 * 60)) % (1000 * 60 * 60)) % (1000 * 60)) / 1000;
        if (hrs < 1)
            time = min + " : " + sec;
        else
            time = hrs + " : " + min + " : " + sec;
        return time;
    }

    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }

    private void exitPlayerView() {
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));

    }

    public void runtimePermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        fetchSongs();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(MainActivity.this, "You canceled show songs", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    private void updatePlayerColors() {
        if (playerView.getVisibility() == View.GONE)
            return;

        BitmapDrawable bitmapDrawable = (BitmapDrawable) artworkView.getDrawable();
        if (bitmapDrawable == null) {
            bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.img_music);
        }

        assert bitmapDrawable != null;
        Bitmap bitmap = bitmapDrawable.getBitmap();

        blurImageView.setImageBitmap(bitmap);
        blurImageView.setBlur(4);

        // we will use palette to extract color from artwork image
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                if (swatch == null) {
                    swatch = palette.getMutedSwatch();
                    if (swatch == null) {
                        swatch = palette.getDominantSwatch();
                    }
                }
                // extract text colors
                assert swatch != null;
                int titleTextColor = swatch.getTitleTextColor();
                int bodyTextColor = swatch.getBodyTextColor();
                int rgbColor = swatch.getRgb();
                // set the color to player views
                getWindow().setStatusBarColor(rgbColor);
                getWindow().setNavigationBarColor(rgbColor);
                artworkView.setBorderColor(rgbColor);
                audioVisualizer.setColor(rgbColor);
                songNameView.setTextColor(titleTextColor);
                playerCloseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                progressView.setTextColor(bodyTextColor);
                durationView.setTextColor(bodyTextColor);
                repeatModeBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipPreviousBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipNextBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                playPauseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                playListBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
            }
        });
    }

    private void userResponsesOnRecordAudioPerm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(recordAudioPermission)) {
                // show an educational UI explaining why we need this permission
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show audio visualizer")
                        .setMessage("Allow this app to display audio visualizer when music is playing")
                        .setPositiveButton("allow", (dialog, which) -> recordAudioPermissionLauncher.launch(recordAudioPermission))
                        .setNegativeButton("No", (dialog, which) -> {
                            Toast.makeText(MainActivity.this, "You denied to show the audio visualizer", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .show();
            } else {
                Toast.makeText(MainActivity.this, "You denied to show the audio visualizer", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // audio visualizer
    private void activateAudioVisualizer() {
        if (ContextCompat.checkSelfPermission(this, recordAudioPermission) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        audioVisualizer.setDensity(20);
        audioVisualizer.setPlayer(player.getAudioSessionId());
    }

    private void fetchSongs() {
        //list to carry songs
        List <Song> songs = new ArrayList <>();
        Uri mediaStoreUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        //define projection
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
        };
        //order
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(mediaStoreUri, projection, null, null, sortOrder)) {
            // cache cursor indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);

            // clear the previous loaded before adding loading again
            while (cursor.moveToNext()) {
                // get the value of a column for a given audio file
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);

                //song uri
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                //album artwork uri
                Uri albumArtworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                //remove .mp3 from the song name
                name = name.substring(0, name.lastIndexOf("."));

                Song song = new Song(name, uri, albumArtworkUri, albumArtworkUri.toString(), size, duration);
                songs.add(song);
            }
            showSongs(songs);
        }
    }

    private void showSongs(List <Song> songs) {
        if (songs.size() == 0) {
            Toast.makeText(this, "No Songs", Toast.LENGTH_SHORT).show();
            return;
        }

        // save songs
        allSongs.clear();
        allSongs.addAll(songs);

        // update toolbar title
        String title = getResources().getString(R.string.app_name) + " - " + songs.size();
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvSongList.setLayoutManager(linearLayoutManager);

        songAdapter = new SongAdapter(this, songs, MainActivity.this);
//        binding.recyclerview.setAdapter(songAdapter);

        // recyclerView animation
        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(1000);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        rvSongList.setAdapter(scaleInAnimationAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_btn, menu);

        MenuItem menuItem = menu.findItem(R.id.searchBtn);
        SearchView searchView = (SearchView) menuItem.getActionView();

        searchSong(searchView);

        return super.onCreateOptionsMenu(menu);
    }

    private void searchSong(SearchView searchView) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }
        });
    }

    private void filterSongs(String query) {
        List <Song> filteredList = new ArrayList <>();
        if (allSongs.size() > 0) {
            for (Song song : allSongs) {
                if (song.getTitle().toLowerCase().contains(query)) {
                    filteredList.add(song);
                }
            }
            if (songAdapter != null) {
                songAdapter.filterSongs(filteredList);
            }
        }
    }

    @Override
    public void onItemClick(int position) {
        startService(new Intent(getApplicationContext(), PlayerService.class));

        playerView.setVisibility(View.VISIBLE);

        if (!player.isPlaying()) {
            player.setMediaItems(getMediaItems(), position, 0);
        } else {
            player.pause();
            player.seekTo(position, 0);
        }

        // prepare and play
        player.prepare();
        player.play();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private List <MediaItem> getMediaItems() {
        List <MediaItem> mediaItems = new ArrayList <>();
        for (Song song : allSongs) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaMetadata(getMetadata(song))
                    .build();

            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private MediaMetadata getMetadata(Song song) {
        return new MediaMetadata.Builder()
                .setTitle(song.getTitle())
                .setArtworkUri(song.getArtworkUri())
                .build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (player.isPlaying()) player.stop();
//
//        player.release();
        if (isBound) {
            unbindService(playerServiceConnection);
            isBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (playerView.getVisibility() == View.VISIBLE) exitPlayerView();
        super.onBackPressed();
    }

    private void bindViews() {
        rvSongList = findViewById(R.id.recyclerview);
        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.btnPlayerClose);
        songNameView = findViewById(R.id.songNameView);
        skipPreviousBtn = findViewById(R.id.btnSkipPrevious);
        skipNextBtn = findViewById(R.id.btnSkipNext);
        playPauseBtn = findViewById(R.id.btnPlayPause);
        repeatModeBtn = findViewById(R.id.btnRepeatMode);
        playListBtn = findViewById(R.id.btnPlayList);

        homeSongNameView = findViewById(R.id.homeSongNameView);
        homeSkipPreviousBtn = findViewById(R.id.btnHomeSkipPrevious);
        homeSkipNextBtn = findViewById(R.id.btnHomeSkipNext);
        homePlayPauseBtn = findViewById(R.id.btnHomePlayPause);

        homeControlWrapper = findViewById(R.id.homeControlWrapper);
        headWrapper = findViewById(R.id.headWrapper);
        artworkWrapper = findViewById(R.id.artworkWrapper);
        seekbarWrapper = findViewById(R.id.seekBarWrapper);
        controlWrapper = findViewById(R.id.controlWrapper);
        audioVisualizerWrapper = findViewById(R.id.audioVisualizerView);

        artworkView = findViewById(R.id.artworkView);
        seekBar = findViewById(R.id.seekBar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);
        audioVisualizer = findViewById(R.id.visualizer);
        blurImageView = findViewById(R.id.blurImageView);
    }
}
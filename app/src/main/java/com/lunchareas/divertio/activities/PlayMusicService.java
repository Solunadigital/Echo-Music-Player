package com.lunchareas.divertio.activities;


import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class PlayMusicService extends Service {

    private static final String TAG = PlayMusicService.class.getName();

    public static final String MUSIC_RESULT = "REQUEST_PROCESSED";
    public static final String MUSIC_POSITION = "POSITION";
    public static final String MUSIC_DURATION = "DURATION";
    public static final String MUSIC_CREATE = "CREATE";
    public static final String MUSIC_CHANGE = "CHANGE";
    public static final String MUSIC_START = "START";
    public static final String MUSIC_PAUSE = "PAUSE";
    public static final String MUSIC_FINISH = "FINISH";
    public static final String PLAYLIST_CREATE = "PLAYLIST_CREATE";

    private Bundle intentCmd;
    private MediaPlayer mp = null;
    private int idx;

    // set up broadcaster to activity to update progress bar
    private LocalBroadcastManager musicUpdater;
    private Thread musicUpdaterThread;

    @Override
    public int onStartCommand(Intent workIntent, int flags, int startId) {
        if (workIntent != null) {
            intentCmd = workIntent.getExtras();
        }

        // create the thread to update progress bar
        musicUpdater = LocalBroadcastManager.getInstance(this);
        musicUpdaterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int songPosition;
                int songDuration;
                do {

                    // change duration if track changes
                    songPosition = mp.getCurrentPosition();
                    songDuration = mp.getDuration();
                    //Log.i(TAG, "Song service position: " + songPosition + "\nSong service duration: " + songDuration);

                    // create and send intent with position and duration
                    Intent songIntent = new Intent(MUSIC_RESULT);
                    songIntent.putExtra(MUSIC_POSITION, songPosition);
                    songIntent.putExtra(MUSIC_DURATION, songDuration);
                    musicUpdater.sendBroadcast(songIntent);
                    musicUpdater.sendBroadcast(songIntent);

                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (mp != null && mp.getCurrentPosition() <= songDuration);
            }
        });

        if (intentCmd != null) {
            if (intentCmd.containsKey(PlayMusicService.MUSIC_CREATE)) {

                // create single song
                Log.i(TAG, "Create Key: " + intentCmd.getString(PlayMusicService.MUSIC_CREATE));
                initMusicPlayer(intentCmd.getString(PlayMusicService.MUSIC_CREATE));
                mp.start();
                musicUpdaterThread.start();

            } else if (intentCmd.containsKey(PlayMusicService.MUSIC_START) && mp != null) {

                // start song
                Log.i(TAG, "Starting music!");
                mp.start();
                musicUpdaterThread.start();

            } else if (intentCmd.containsKey(PlayMusicService.MUSIC_PAUSE) && mp != null) {

                // pause song
                Log.i(TAG, "Pausing music!");
                mp.pause();

            } else if (intentCmd.containsKey(PlayMusicService.MUSIC_CHANGE) && mp != null) {

                // change song location
                int newPosition = intentCmd.getInt(PlayMusicService.MUSIC_CHANGE);
                mp.seekTo(newPosition);
                Log.i(TAG, "Changing to new position!");

            } else if (intentCmd.containsKey(PlayMusicService.PLAYLIST_CREATE)) {

                // create playlist queue
                Log.i(TAG, "Beginning playlist queue!");
                String[] songPathList = intentCmd.getStringArray(PlayMusicService.PLAYLIST_CREATE);
                beginPlaylistQueue(songPathList);

            } else {
                Log.e(TAG, "Command sent to PlayMusicService not found.");
                if (intentCmd.isEmpty()) {
                    Log.e(TAG, "No command sent, bundle empty.");
                }
            }
        }

        return START_STICKY;
    }

    private void beginPlaylistQueue(final String[] songPathList) {

        // Play the first song
        initMusicPlayer(songPathList[0]);
        mp.start();
        musicUpdaterThread.start();

        // Setup for completion
        idx = 1;
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (idx < songPathList.length) {
                    try {
                        mp.reset();
                        mp.setDataSource(songPathList[idx]);
                        mp.prepare();
                        mp.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    musicUpdaterThread.start();
                    idx += 1;
                    Log.i(TAG, "Playing next song, number " + Integer.toString(idx));
                } else {
                    Log.i(TAG, "Finished playlist.");
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "PlayMusicService destroyed...");
        mp.release();
        mp = null;
        musicUpdaterThread.interrupt();
    }

    public void initMusicPlayer(String path) {
        if (path != null) {
            mp = MediaPlayer.create(this, Uri.parse(path));
            if (mp != null) {
                mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
        }
    }

    public Thread getMusicUpdaterThread() {
        return musicUpdaterThread;
    }

    public void setMusicUpdaterThread (Thread t) {
        musicUpdaterThread = t;
    }

    /*
    @Override
    protected void onHandleIntent(Intent workIntent) {
        String dataString = workIntent.getDataString();
        Integer songId = new Integer(dataString);

        songPlayer = MediaPlayer.create(this, songId);
        songPlayer.setLooping(true);
        songPlayer.start();
    }
    */
}
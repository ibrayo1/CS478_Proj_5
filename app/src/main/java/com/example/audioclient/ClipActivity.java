package com.example.audioclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.clipcommon.ClipGenerator;

import java.util.ArrayList;

public class ClipActivity extends AppCompatActivity {

    protected static final String TAG = "ClipServiceUser";
    private ClipGenerator mClipGeneratorService = null;
    private boolean mIsBound = false;
    private ArrayList<String> audioList = new ArrayList<>();
    private ArrayList<String> durationList = new ArrayList<>();

    // for updating the recycler view
    private ListAdapter adapter;
    private RecyclerView mRecyclerView;

    // declare all buttons
    private Button startButton;
    private Button playButton;
    private Button pauseButton;
    private Button resumeButton;
    private Button stopPlayback;
    private Button stopButton;

    private int clip = -1;
    // Intent used for starting the MusicService
    private Intent ClipServiceIntent;
    private ClientReceiver mClientReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip);

        // all the buttons
        startButton = (Button) findViewById(R.id.start_service_button);
        playButton = (Button) findViewById(R.id.play_playback_btn);
        pauseButton = (Button) findViewById(R.id.pause_playback_btn);
        resumeButton = (Button) findViewById(R.id.resume_playback_btn);
        stopPlayback = (Button) findViewById(R.id.stop_playback_btn);
        stopButton = (Button) findViewById(R.id.stop_service_button);

        // disable the buttons that we dont need
        stopButton.setEnabled(false);
        playButton.setEnabled(false);
        pauseButton.setEnabled(false);
        resumeButton.setEnabled(false);
        stopPlayback.setEnabled(false);

        // set the list view
        mRecyclerView = (RecyclerView) findViewById(R.id.audio_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        populateAudioList();
        populateDurationList();

        // create new list adapter and set recycler view
        adapter = new ListAdapter(this, audioList, durationList);
        mRecyclerView.setAdapter(adapter);
        findViewById(R.id.overlay).setVisibility(View.VISIBLE);

        // register broadcast receiver
        mClientReceiver = new ClientReceiver(ClipActivity.this);
        IntentFilter filter = new IntentFilter("unbind");
        registerReceiver(mClientReceiver, filter);

        // bind to the service when this is pressed
        startButton.setOnClickListener((v) -> {
            if (!mIsBound) {

                ClipServiceIntent = new Intent(ClipGenerator.class.getName());
                ResolveInfo info = getPackageManager().resolveService(ClipServiceIntent, 0);
                ClipServiceIntent.setComponent(new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplicationContext().startForegroundService(ClipServiceIntent);
                    getApplicationContext().bindService(ClipServiceIntent, mConnection, 0);
                }

                // reset items in list to default state
                adapter = new ListAdapter(this, audioList, durationList);
                mRecyclerView.setAdapter(adapter);
                findViewById(R.id.overlay).setVisibility(View.GONE);

            }
        });

        playButton.setOnClickListener((v) -> {
            if(mIsBound && clip != -1){
                try{
                    mClipGeneratorService.playAudio(clip);
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    stopPlayback.setEnabled(true);
                    findViewById(R.id.overlay).setVisibility(View.VISIBLE);
                } catch (RemoteException re){
                    re.printStackTrace();
                }
            } else {
                getApplicationContext().bindService(ClipServiceIntent, mConnection, 0);
                mIsBound = true;
                try{
                    mClipGeneratorService.playAudio(clip);
                    playButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    stopPlayback.setEnabled(true);
                    findViewById(R.id.overlay).setVisibility(View.VISIBLE);
                } catch (RemoteException re){
                    re.printStackTrace();
                }
            }
        });

        pauseButton.setOnClickListener((v) -> {
            if(mIsBound){
                try {
                    mClipGeneratorService.pauseAudio();
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                } catch (RemoteException re){
                    re.printStackTrace();
                }
            }
        });

        resumeButton.setOnClickListener((v) -> {
            if(mIsBound){
                try {
                    mClipGeneratorService.resumeAudio();
                    resumeButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                } catch (RemoteException re){
                    re.printStackTrace();
                }
            }
        });

        stopPlayback.setOnClickListener((v) -> {
            if(mIsBound){
                try {
                    // unbind from activity and stop audio
                    mClipGeneratorService.stopAudio();
                    getApplicationContext().unbindService(mConnection);
                    mIsBound = false;

                    // disable the respective buttons
                    stopPlayback.setEnabled(false);
                    pauseButton.setEnabled(false);
                    stopButton.setEnabled(true);
                    playButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    startButton.setEnabled(false);

                    //reset items in list to default state
                    adapter = new ListAdapter(this, audioList, durationList);
                    mRecyclerView.setAdapter(adapter);
                    findViewById(R.id.overlay).setVisibility(View.GONE);

                } catch (RemoteException re){
                    re.printStackTrace();
                }
            }
        });

    }

    // dialog which pops up when user clicks on stop service button
    public void showDialog(View view){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning!");
        builder.setMessage("Are you sure you want to stop the service? Playback will be stopped");
        builder.setPositiveButton("YES", (dialog, which) -> {
            // reset items in list to default state
            adapter = new ListAdapter(ClipActivity.this, audioList, durationList);
            mRecyclerView.setAdapter(adapter);
            findViewById(R.id.overlay).setVisibility(View.GONE);

            // disable the respective buttons
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            resumeButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopPlayback.setEnabled(false);
            playButton.setEnabled(false);

            // bind service again and then stopService
            if(!mIsBound)
                getApplicationContext().bindService(ClipServiceIntent, mConnection, 0);
            Intent stopIntent = new Intent("StopService");
            sendBroadcast(stopIntent);
        });
        builder.setNegativeButton("CANCEL", (dialog, which) -> { dialog.dismiss(); });
        builder.create().show();
    }

    // method which is called by broadcast receiver to unbind service when clip is completed
    public void clipCompleted(){
        // unbind service
        getApplicationContext().unbindService(mConnection);
        mIsBound = false;

        // disabled and enable the specified buttons
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        resumeButton.setEnabled(false);
        pauseButton.setEnabled(false);
        stopPlayback.setEnabled(false);
        playButton.setEnabled(false);

        //reset items in list to default state
        adapter = new ListAdapter(this, audioList, durationList);
        mRecyclerView.setAdapter(adapter);
        findViewById(R.id.overlay).setVisibility(View.GONE);
    }

    public void setClip(int pos){
        this.clip = pos;
        playButton.setEnabled(true);
    }

    @Override
    protected void onStop(){
        if(isFinishing() && mIsBound)
            getApplicationContext().unbindService(mConnection);
        super.onStop();
    }

    @Override
    protected void onPause(){
        if(isFinishing() && mIsBound)
            getApplicationContext().unbindService(mConnection);
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        Log.i("onDestroy", "onDestroy is being called from ClipActivity");
        if(mIsBound)
            getApplicationContext().unbindService(mConnection);
        unregisterReceiver(mClientReceiver);
        super.onDestroy();
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder iservice) {

            mClipGeneratorService = ClipGenerator.Stub.asInterface(iservice);
            mIsBound = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        }

        public void onServiceDisconnected(ComponentName className) {

            mClipGeneratorService = null;
            Toast.makeText(getBaseContext(), "service disconnected!",Toast.LENGTH_SHORT).show();
            findViewById(R.id.overlay).setVisibility(View.VISIBLE);

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            resumeButton.setEnabled(false);
            pauseButton.setEnabled(false);
            stopPlayback.setEnabled(false);
            playButton.setEnabled(false);

            mIsBound = false;

        }
    };

    public void populateAudioList(){
        audioList.add("strongest.mp3");
        audioList.add("healer.mp3");
        audioList.add("badnews.m4a");
        audioList.add("Buddy.mp3");
        audioList.add("Freedom.mp3");
        audioList.add("Dubstep.mp3");
    }

    public void populateDurationList(){
        durationList.add("1:45 mins");
        durationList.add("0:45 mins");
        durationList.add("2:02 mins");
        durationList.add("2:02 mins");
        durationList.add("2:20 mins");
        durationList.add("2:04 mins");
    }


}

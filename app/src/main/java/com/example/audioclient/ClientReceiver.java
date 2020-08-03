package com.example.audioclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ClientReceiver extends BroadcastReceiver {

    ClipActivity main = null;

    public ClientReceiver(ClipActivity main){
        this.main = main;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("unbind")){
            main.clipCompleted();
            Toast.makeText(context, "Clip Completed", Toast.LENGTH_LONG).show();
        }
    }

}

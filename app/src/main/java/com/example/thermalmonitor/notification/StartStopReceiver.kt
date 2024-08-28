package com.example.thermalmonitor.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import timber.log.Timber

class StartStopReceiver : BroadcastReceiver()   {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("StartStopReceiver", "onReceive called")
        if (context != null && intent != null) {
            when(intent.action){
                "com.example.thermalmonitor.ACTION_START" -> {
                    //start service
                    Timber.tag("StartStopReceiver").i("Start button pressed")

                }
                "com.example.thermalmonitor.ACTION_STOP" ->
                    //stop service
                    Log.i("StartStopReceiver", "Stop button pressed")
            }
        }
    }
}

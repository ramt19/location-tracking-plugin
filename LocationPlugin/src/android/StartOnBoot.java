package com.ram.cordova.plugin;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.util.Calendar;
/**
 * Created by Himanshu on 16-06-2018.
 */

public class StartOnBoot extends BroadcastReceiver {
	
	private boolean startOnBoot;
	private int timer;
    @Override
    public void onReceive(Context context, Intent intent) {
        
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            startOnBoot = prefs.getBoolean("startOnBoot",true);
			timer = prefs.getInt("timer",1000*60*30);
			

            if(startOnBoot) {
                Intent service = new Intent(context, UpdaterService.class);
				Calendar cal = Calendar.getInstance();
				PendingIntent penintent = PendingIntent.getService(context, 0, service, 0);
				AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),timer, penintent);

                Log.i("locationservice", "BroadcastReciever");
            }
    }
}

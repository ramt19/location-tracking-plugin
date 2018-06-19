package com.ram.cordova.plugin;
// The native Toast API
import android.widget.Toast;
// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.util.Log;
import android.app.AlarmManager;
import android.app.PendingIntent;
import java.util.Calendar;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import java.util.ArrayList;
import android.content.pm.PackageManager;


import java.util.List;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;





public class LocationPlugin extends CordovaPlugin {


  private String key;
  private String URL;
  private String contentType;
  private int timer;
  private boolean debug;
  private boolean startOnBoot;


 @Override
 public boolean execute(String action, JSONArray args,final CallbackContext callbackContext) {
        
        Log.i("locationservice","execute");


        //Toast.makeText(cordova.getActivity(), action, Toast.LENGTH_LONG).show();
        if (action.equals("start")) {

          try {
            JSONObject options = args.getJSONObject(0);
              
              key = options.getString("key");
              URL = options.getString("url");
              contentType = options.getString("contentType");
              timer = options.getInt("timer");
              debug = options.getBoolean("debug");
			  startOnBoot = options.getBoolean("startOnBoot");
              
              Log.i("locationservice",options.getString("key"));
              Log.i("locationservice",options.getString("url"));
              Log.i("locationservice",options.getString("contentType"));
              Log.i("locationservice",options.getString("timer"));
              Log.i("locationservice",options.getString("debug"));
			   Log.i("locationservice",options.getString("startOnBoot"));
              
          } catch (JSONException e) {
              Log.i("locationservice",e.toString());
          }

            start();
            // Send a positive result to the callbackContext
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }
        else if(action.equals("stop")){
            Activity activity = this.cordova.getActivity();
            Intent intent = new Intent(activity, UpdaterService.class);
            activity.stopService(intent);
            // Send a positive result to the callbackContext
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            callbackContext.sendPluginResult(pluginResult);


            return true;
        }

        else{
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }
  }

    public void start(){


        Activity activity = this.cordova.getActivity();
        Intent intent = new Intent(activity, UpdaterService.class);
        Context context = this.cordova.getActivity().getApplicationContext();

        // Request for necessary permission
        List<String> allPermission = new ArrayList<String>();
        if(ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){

            allPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            allPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            allPermission.add(android.Manifest.permission.READ_PHONE_STATE);
			allPermission.add(android.Manifest.permission.ACCESS_NETWORK_STATE);


            ActivityCompat.requestPermissions(activity,allPermission.toArray(new String[allPermission.size()]),1 );
          }

        // Set the variables
        setVariable();


        Calendar cal = Calendar.getInstance();

        PendingIntent penintent = PendingIntent.getService(activity, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),timer, penintent);
    }

    public void setVariable(){
		
		SharedPreferences myPreferences = PreferenceManager.getDefaultSharedPreferences(this.cordova.getActivity());
        SharedPreferences.Editor myEditor = myPreferences.edit();

        myEditor.putString("autKey",key);
        myEditor.putString("contentType",contentType);
        myEditor.putBoolean("debug",debug);
        myEditor.putString("URL",URL);
		myEditor.putBoolean("startOnBoot",startOnBoot);
		
		myEditor.commit();
		
       
    }

   
    
}







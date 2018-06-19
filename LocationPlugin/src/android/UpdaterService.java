package com.ram.cordova.plugin;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.Toast;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.ram.cordova.plugin.LocationDatabase;
/**
 * Created by Himanshu on 31-05-2018.
 */

public class UpdaterService extends Service {


    private String phoneType = null;
    private int mcc = 0;
    private int mnc = 0;
    private int lac = 0;
    private int cid = 0;
    private double latitude = 0;
    private double longitude = 0;
    private String carrier;
    TelephonyManager telephonyManager;

    private static String autKey;
    private static String URL;
    private static String contentType ;
    private static boolean debug ;
    private static boolean startOnBoot;
    private static String date;
    private static boolean permission = true;



    public void onCreate() {
        Log.i("locationservice","onCreate");


        int phoneTypeInt;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        phoneTypeInt = telephonyManager.getPhoneType();
        phoneType = phoneTypeInt == TelephonyManager.PHONE_TYPE_GSM ? "gsm" : phoneType;
        phoneType = phoneTypeInt == TelephonyManager.PHONE_TYPE_CDMA ? "cdma" : phoneType;
        carrier = telephonyManager.getNetworkOperatorName();


        try {
            if (phoneType != null) {
                Log.e("radiotype", phoneType);
            }
        } catch (Exception e) {

        }

    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        Connection conn = new Connection();
        conn.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.i("locationservice","onstartcommand");

        return START_STICKY;
    }

    public void onDestroy() {

        Log.i("locationservice","ondestroy");

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public class Connection extends AsyncTask<String, Void, String> {

        StringBuilder resp = new StringBuilder();


        final Handler gpsHandler = new Handler();
        final Runnable getGPS = new Runnable() {
            @Override
            public void run() {
                final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location mlocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    }

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            locationManager.removeUpdates(this);
                        }
                        @Override
                        public void onStatusChanged(String s, int i, Bundle bundle) {}

                        @Override
                        public void onProviderEnabled(String s) {}

                        @Override
                        public void onProviderDisabled(String s) {}
                    });

                    latitude = mlocation.getLatitude();
                    longitude = mlocation.getLongitude();

                   showToast(new StringBuilder(String.valueOf(latitude)).append("  ").append(String.valueOf(longitude)));
                }
                else{
					latitude = 0;
                    longitude = 0;
                    showToast(new StringBuilder("GPS provider disabled"));
                }
                gpsHandler.removeCallbacks(this);
            }

        };

        protected void onPreExecute() {
            Log.i("locationservice","onPreExecute");
            Log.i("locationservice",String.valueOf(debug));
        }

        @Override
        protected String doInBackground(String... strings) {
            Log.i("locationservice","doInBackground");

            Context ctx = getApplicationContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            autKey = prefs.getString("autKey",null);
            URL = prefs.getString("URL",null);
            contentType = prefs.getString("contentType",null);

            Log.i("locationservice",autKey);
            Log.i("locationservice",URL);
            Log.i("locationservice",contentType);

            // Get the value of mcc, mnc, lac, cid
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String networkOperator = telephonyManager.getNetworkOperator();
            mcc = Integer.parseInt(networkOperator.substring(0, 3));
            mnc = Integer.parseInt(networkOperator.substring(3));
            try {
                final GsmCellLocation location = (GsmCellLocation) telephonyManager.getCellLocation();
                cid = location.getCid();
                lac = location.getLac();

                onGPSHandlerStart();

                Log.i("locationservice","permission given");
                permission = true;
            }
            catch (Exception e){
                permission = false;
				showToast(new StringBuilder("permission denied"));
                Log.i("locationservice","permission denied");
				 cid = 0;
                lac = 0;
                e.printStackTrace();
            }


            DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
            date = df.format(Calendar.getInstance().getTime());

            // For debug purpose
            showToast(new StringBuilder(date).append("\n").append(latitude).append("\n").append(longitude));
            showToast(new StringBuilder(date).append("\n").append(mcc).append("\n").append(mnc).append("\n").append(lac).append("\n").append(cid));

            if(!isConnected(getApplicationContext())){
                showToast(new StringBuilder("internet unavailable"));

                storeInDatabase();

            }
            else {
				
				sendToServer();
                showToast(new StringBuilder("internet available"));
				
				Log.i("locationservice",String.valueOf(autKey));
                Log.i("locationservice",String.valueOf(date));
                Log.i("locationservice",String.valueOf(mcc));
                Log.i("locationservice",String.valueOf(mnc));
                Log.i("locationservice",String.valueOf(lac));
                Log.i("locationservice",String.valueOf(cid));
                Log.i("locationservice",String.valueOf(latitude));
                Log.i("locationservice",String.valueOf(longitude));
                Log.i("locationservice",String.valueOf(permission));
                // Data to be send to the server along with LAT and LONG
                String data = null;
                try {
                    data = URLEncoder.encode("Authkey", "UTF-8")
                            + "=" + URLEncoder.encode(new String(autKey), "UTF-8");
                    data += "&" + URLEncoder.encode("dateTime", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(date)), "UTF-8");
                    data += "&" + URLEncoder.encode("mcc", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(mcc)), "UTF-8");
                    data += "&" + URLEncoder.encode("mnc", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(mnc)), "UTF-8");
                    data += "&" + URLEncoder.encode("lac", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(lac)), "UTF-8");
                    data += "&" + URLEncoder.encode("cid", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(cid)), "UTF-8");
                    data += "&" + URLEncoder.encode("latitude", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(latitude)), "UTF-8");
                    data += "&" + URLEncoder.encode("longitude", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(longitude)), "UTF-8");
                    data += "&" + URLEncoder.encode("permission", "UTF-8") + "="
                            + URLEncoder.encode(new String(String.valueOf(permission)), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }


                // Send data to the server
                String sb = connectAndSend("POST",URL,String.valueOf(data),contentType);


                showToast(new StringBuilder().append(sb));

                Log.i("locationservice",sb.toString());

                




            }

            return resp.toString();
        }

        protected void onPostExecute(String resp) {
            Log.i("locationservice","onPostExecute");


        }


        public void onGPSHandlerStart() {
            gpsHandler.post(getGPS);
        }


    }



    public String connectAndSend(String method, String url, String data, String contentType){

        HttpURLConnection connVariable;
        StringBuilder response = new StringBuilder();
        try {
            URL connectionURL = new URL(url);

            connVariable = (HttpURLConnection) connectionURL.openConnection();
            connVariable.setDoOutput(true);
            connVariable.setRequestMethod(method);
            connVariable.setRequestProperty("Content-Type", contentType);
        /*connVariable.setRequestProperty("Accpet", "application/json");  */ //header

            connVariable.connect();
            Writer wr = new BufferedWriter(new OutputStreamWriter(connVariable.getOutputStream()));
            wr.write(String.valueOf(data));
            wr.close();


            BufferedReader reader = null;
            reader = new BufferedReader(new InputStreamReader(connVariable.getInputStream()));
            String line = null;
            // Read Server Response
            while ((line = reader.readLine()) != null) {
                // Append server response in string
                response.append(line + "\n");
            }

            connVariable.disconnect();

        } catch (MalformedURLException e) {

        } catch (IOException e) {

        }
        return String.valueOf(response);
    }

    public void showToast(final StringBuilder showString){

        Context ctx = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        debug = prefs.getBoolean("debug",false);

        if(debug) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),showString.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public boolean isConnected(Context context) {
        ConnectivityManager
                cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
    }

    public void storeInDatabase(){
        LocationDatabase mLocationDatabase = new LocationDatabase(getApplicationContext());
        SQLiteDatabase db = mLocationDatabase.getWritableDatabase();

        if(!mLocationDatabase.isTableExists(mLocationDatabase.getReadableDatabase())){
            mLocationDatabase.onCreate(db);
        }
			
			Log.i("locationservice",String.valueOf(autKey));
                Log.i("locationservice",String.valueOf(date));
                Log.i("locationservice",String.valueOf(mcc));
                Log.i("locationservice",String.valueOf(mnc));
                Log.i("locationservice",String.valueOf(lac));
                Log.i("locationservice",String.valueOf(cid));
                Log.i("locationservice",String.valueOf(latitude));
                Log.i("locationservice",String.valueOf(longitude));
                Log.i("locationservice",String.valueOf(permission));
				
				
        mLocationDatabase.insert(autKey, date, mcc, mnc, lac, cid,latitude,longitude, String.valueOf(permission));
        db.close();
		Log.i("locationservice","data stored");
		showToast(new StringBuilder("data stored"));
    }

    public void sendToServer(){
        LocationDatabase mLocationDatabase = new LocationDatabase(getApplicationContext());
        SQLiteDatabase db = mLocationDatabase.getWritableDatabase();
        if(mLocationDatabase.anyData(db)) {
            String data = null;
            Cursor cursor = mLocationDatabase.read();
            if (cursor.moveToFirst()) {
                do {
                    try {
                        data = URLEncoder.encode("Authkey", "UTF-8")
                                + "=" + URLEncoder.encode(new String(cursor.getString(1)), "UTF-8");
                        data += "&" + URLEncoder.encode("dateTime", "UTF-8") + "="
                                + URLEncoder.encode(new String(cursor.getString(2)), "UTF-8");
                        data += "&" + URLEncoder.encode("mcc", "UTF-8") + "="
                                + URLEncoder.encode(new String(String.valueOf(cursor.getInt(3))), "UTF-8");
                        data += "&" + URLEncoder.encode("mnc", "UTF-8") + "="
                                + URLEncoder.encode(new String(String.valueOf(cursor.getInt(4))), "UTF-8");
                        data += "&" + URLEncoder.encode("lac", "UTF-8") + "="
                                + URLEncoder.encode(new String(String.valueOf(cursor.getInt(5))), "UTF-8");
                        data += "&" + URLEncoder.encode("cid", "UTF-8") + "="
                                + URLEncoder.encode(new String(String.valueOf(cursor.getInt(6))), "UTF-8");
                        data += "&" + URLEncoder.encode("latitude", "UTF-8") + "="
                                + URLEncoder.encode(new String(String.valueOf(cursor.getString(7))), "UTF-8");
                        data += "&" + URLEncoder.encode("latitude", "UTF-8") + "="
                                + URLEncoder.encode(new String(String.valueOf(cursor.getString(8))), "UTF-8");
                        data += "&" + URLEncoder.encode("latitude", "UTF-8") + "="
                                + URLEncoder.encode(new String(String.valueOf(cursor.getString(9))), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    String sb = connectAndSend("POST", URL, String.valueOf(data), contentType);
                    Log.i("locationservice", sb.toString());

                    mLocationDatabase.delete(cursor.getInt(0));
                } while (cursor.moveToNext());
            }
			showToast(new StringBuilder("data send"));
            db.close();
        }
        else
            Log.i("locationservice","no data in database");
		
    }



}




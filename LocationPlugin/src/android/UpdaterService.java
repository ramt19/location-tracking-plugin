package com.org.cordova.plugin;

import android.Manifest;
import android.app.Service;
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
import android.database.Cursor;;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import com.org.cordova.plugin.LocationDatabase;

public class UpdaterService extends Service {

    private String phoneType = null;
    private int mcc = -1;
    private int mnc = -1;
    private int lac = -1;
    private int cid = -1;
    private double latitude = -1;
    private double longitude = -1;
    private String carrier;
    private TelephonyManager telephonyManager;
    private static String URL;
    private static String method;
    private static String headers;
    private static String contentType;
    private static boolean debug ;
    private static String locationTime;
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
            e.printStackTrace();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        //Start the Async Task
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
        // Handler to check for GPS provider and Location Permission and get Latitude and Longitude
        final Handler gpsHandler = new Handler();
        final Runnable getGPS = new Runnable() {
            @Override
            public void run() {
                final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location mlocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // DO NOTHING
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
                    latitude = -1;
                    longitude = -1;
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
            // Get Passed Values through Shared Preferences
            Context ctx = getApplicationContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

            URL = prefs.getString("URL",null);
            method = prefs.getString("method","POST");
            headers = prefs.getString("headers",null);
            contentType = prefs.getString("contentType","application/x-www-form-urlencoded");

            Log.i("locationservice",headers);
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
                // If location permission is turned off manually after installation
                permission = false;
                showToast(new StringBuilder("permission denied"));
                Log.i("locationservice","permission denied");
                cid = -1;
                lac = -1;
                e.printStackTrace();
            }

            DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm");
            locationTime = df.format(Calendar.getInstance().getTime());
            showToast(new StringBuilder(locationTime).append("\n").append(latitude).append("\n").append(longitude));
            showToast(new StringBuilder(locationTime).append("\n").append(mcc).append("\n").append(mnc).append("\n").append(lac).append("\n").append(cid));

            if(!isConnected(getApplicationContext())){
                // Store data in database if internet unavailable
                showToast(new StringBuilder("internet unavailable"));
                storeInDatabase();
            }
            else {
                // Send the data stored in database
                sendToServer();

                showToast(new StringBuilder("internet available"));
                Log.i("locationservice",String.valueOf(headers));
                Log.i("locationservice",String.valueOf(locationTime));
                Log.i("locationservice",String.valueOf(mcc));
                Log.i("locationservice",String.valueOf(mnc));
                Log.i("locationservice",String.valueOf(lac));
                Log.i("locationservice",String.valueOf(cid));
                Log.i("locationservice",String.valueOf(latitude));
                Log.i("locationservice",String.valueOf(longitude));
                Log.i("locationservice",String.valueOf(permission));
                // Data to be send to the server along with LAT and LONG
                String data = dataInContentType(contentType,locationTime,mcc,mnc,lac,cid,String.valueOf(latitude),String.valueOf(longitude),String.valueOf(permission));
                // Send data to the server
                String sb = connectAndSend(method,URL,headers,data);
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

    // Common function to send through HTTP
    public String connectAndSend(String method, String url,String headers, String data){
        HttpURLConnection connVariable;
        StringBuilder response = new StringBuilder();
        try {
            URL connectionURL = new URL(url);
            connVariable = (HttpURLConnection) connectionURL.openConnection();
            connVariable.setDoOutput(true);
            connVariable.setRequestMethod(method);
            connVariable.setRequestProperty("Header",headers);
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
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return String.valueOf(response);
    }

    // Debugger function to show Toast
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

    // Check Internet Availability
    public boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
    }

    // Store data in database
    public void storeInDatabase(){
        LocationDatabase mLocationDatabase = new LocationDatabase(getApplicationContext());
        SQLiteDatabase db = mLocationDatabase.getWritableDatabase();

        if(!mLocationDatabase.isTableExists(mLocationDatabase.getReadableDatabase())){
            mLocationDatabase.onCreate(db);
        }
        Log.i("locationservice",String.valueOf(headers));
        Log.i("locationservice",String.valueOf(locationTime));
        Log.i("locationservice",String.valueOf(mcc));
        Log.i("locationservice",String.valueOf(mnc));
        Log.i("locationservice",String.valueOf(lac));
        Log.i("locationservice",String.valueOf(cid));
        Log.i("locationservice",String.valueOf(latitude));
        Log.i("locationservice",String.valueOf(longitude));
        Log.i("locationservice",String.valueOf(permission));

        mLocationDatabase.insert(headers, locationTime, mcc, mnc, lac, cid,latitude,longitude, String.valueOf(permission));
        db.close();
        Log.i("locationservice","data stored");
        showToast(new StringBuilder("data stored"));
    }

    // Send the database data to Server
    public void sendToServer(){
        LocationDatabase mLocationDatabase = new LocationDatabase(getApplicationContext());
        SQLiteDatabase db = mLocationDatabase.getWritableDatabase();
        if(mLocationDatabase.anyData(db)) {
            String data = null;
            Cursor cursor = mLocationDatabase.read();
            if (cursor.moveToFirst()) {
                do {    data = dataInContentType(contentType,

                        cursor.getString(2),
                        cursor.getInt(3),
                        cursor.getInt(4),
                        cursor.getInt(5),
                        cursor.getInt(6),
                        String.valueOf(cursor.getFloat(7)),
                        String.valueOf(cursor.getFloat(8)),
                        String.valueOf(cursor.getFloat(9))
                );
                    String sb = connectAndSend(method, URL, cursor.getString(1), String.valueOf(data));
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

    public String dataInContentType(String type, String locationTime, int mcc, int mnc, int lac, int cid, String latitude, String longitude, String permission ){

        if(type.equals("application/x-www-form-urlencoded")){
            String data = null;
            try {

                data = "&" + URLEncoder.encode("locationTime", "UTF-8") + "="
                        + URLEncoder.encode(new String(String.valueOf(locationTime)), "UTF-8");
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

            return String.valueOf(data);
        }

        if(type.equals("application/json")){
            JSONObject data = new JSONObject();
            try {

                data.put("locationTime",locationTime);
                data.put("MCC", mcc);
                data.put("MNC", mnc);
                data.put("LAC", lac);
                data.put("CID", cid);
                data.put("lat",latitude);
                data.put("long",longitude);
                data.put("permission",permission);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return String.valueOf(data);
        }
        return null;
    }
}




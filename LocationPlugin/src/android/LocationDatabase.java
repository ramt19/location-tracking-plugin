package com.ram.cordova.plugin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Himanshu on 14-06-2018.
 */

public class LocationDatabase extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Location_db";
    public static final String TABLE_NAME = "location";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_AUTHKEY = "authkey";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_MCC = "mcc";
    public static final String COLUMN_MNC = "mnc";
    public static final String COLUMN_LAC = "lac";
    public static final String COLUMN_CID = "cid";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_PERMISSION = "permission";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + "(" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"+
            COLUMN_AUTHKEY +" TEXT," +
            COLUMN_DATE + " TEXT," +
            COLUMN_MCC + " INT,"+
            COLUMN_MNC + " INT," +
            COLUMN_LAC + " INT," +
            COLUMN_CID + " INT," +
            COLUMN_LATITUDE + " FLOAT," +
            COLUMN_LONGITUDE + " FLOAT," +
            COLUMN_PERMISSION + " TEXT );"
            ;

    public LocationDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(sqLiteDatabase);
    }

    public long insert(String authkey, String date, int mcc, int mnc, int lac, int cid, double latitude, double longitude,
                       String permission ){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_AUTHKEY,authkey);
        values.put(COLUMN_DATE,date);
        values.put(COLUMN_MCC,mcc);
        values.put(COLUMN_MNC,mnc);
        values.put(COLUMN_LAC,lac);
        values.put(COLUMN_CID,cid);
        values.put(COLUMN_LATITUDE,latitude);
        values.put(COLUMN_LONGITUDE,longitude);
        values.put(COLUMN_PERMISSION,permission);

        long id = db.insert(TABLE_NAME,null,values);

        db.close();

        return id;
    }

    public Cursor read(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM "+TABLE_NAME + ";",null);
        return  res;
    }

    public void delete(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM "+TABLE_NAME+" where id = "+id+";";
        db.execSQL(query);

    }

    public void onTableDelete(SQLiteDatabase db){
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME );
    }

    public boolean isTableExists( SQLiteDatabase mDatabase) {


        Cursor cursor = mDatabase.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"
                + TABLE_NAME + "'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;


    }
	
	 public boolean anyData(SQLiteDatabase mDatabase){

        Cursor cursor = mDatabase.rawQuery("SELECT * FROM "+TABLE_NAME +";",null);
        if(cursor!=null){
            if(cursor.getCount()>0){
                cursor.close();
                return true;
            }
        }
        return false;

    }
}

package com.azam.android.salaattimes;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.sentry.Sentry;

/**
 * Created by zaheer on 9/14/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    //The Android's default system path of your application database.

    private static final String DB_NAME = "salaattimes";

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DatabaseHelper(Context context) {

        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }

    private String getDatabaseFilename() throws PackageManager.NameNotFoundException {
        String DB_PATH_SUFFIX = "/databases/";
        return this.myContext.getPackageManager().getPackageInfo(myContext.getPackageName(), 0).applicationInfo.dataDir + DB_PATH_SUFFIX + DB_NAME;
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        createDataBase();
        return super.getReadableDatabase();
    }
    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public synchronized void createDataBase() {

        boolean dbExist = checkDataBase();

        if(dbExist){
            //do nothing - database already exist
            Log.i("DatabaseHelper", "database exists, no need to create");
        }else{
            Log.i("DatabaseHelper", "database needs to be created");

            try {

                copyDataBase();

            } catch (IOException e) {
                Sentry.captureException(e);
                throw new RuntimeException("Error copying database: " + e);

            }
        }

    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){

        SQLiteDatabase checkDB = null;
        boolean recent = true;
        try{
            String myPath = getDatabaseFilename();
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor = checkDB.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = 'salaat_version'", null);
            if(cursor!=null) {
                if(cursor.getCount()<=0) {
                    recent = false;
                }
                cursor.close();
            }
            if (recent) {
                Cursor version_cursor = checkDB.rawQuery("SELECT version FROM salaat_version", null);
                if(version_cursor.getCount()<=0) {
                    recent = false;
                }
                else {
                    version_cursor.moveToFirst();
                    int CURRENT_VERSION = 8;
                    if (version_cursor.getInt(0) < CURRENT_VERSION) {
                        recent = false;
                    }
                }
                version_cursor.close();

            }
        }catch(SQLiteException e){
            //database doesn't exist yet.
        }
        catch (PackageManager.NameNotFoundException e) {
            Sentry.captureException(e);
            throw new Error("Cannot find name");
        }

        if(checkDB != null){

            checkDB.close();

        }

        return checkDB != null && recent;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException{

        Log.i("DatabaseHelper", "First run of version, copying db");
        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        try {
            String outFileName = getDatabaseFilename();
            Log.i("DatabaseHelper", "starting to copy db from " + DB_NAME + " to " + outFileName);
            super.getReadableDatabase();
            super.close();
            //Open the empty db as the output stream
            OutputStream myOutput = new FileOutputStream(outFileName);
            Log.i("DatabaseHelper", "copy step 0");

            //transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > -1) {
                myOutput.write(buffer, 0, length);
                //Log.i("DatabaseHelper", "wrote " + length + " bytes available " + myInput.available());
            }

            //Close the streams
            myOutput.flush();
            myOutput.close();
            myInput.close();
            Log.i("DatabaseHelper", "copy db complete");

        }
        catch (PackageManager.NameNotFoundException e) {
            Sentry.captureException(e);
            throw new Error("Cannot find name");
        }
    }

    public void openDataBase() {
        Log.i("DatabaseHelper", "openDatabase");
        myDataBase = getReadableDatabase();

    }

    @Override
    public synchronized void close() {

        if(myDataBase != null)
            myDataBase.close();

        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    // Add your public helper methods to access and get content from the database.
    // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
    // to you to create adapters for your views.

}
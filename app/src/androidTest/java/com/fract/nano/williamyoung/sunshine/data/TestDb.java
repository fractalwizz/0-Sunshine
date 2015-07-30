package com.fract.nano.williamyoung.sunshine.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.test.AndroidTestCase;
import java.util.HashSet;

public class TestDb extends AndroidTestCase {
    public static final String LOG_TAG = TestDb.class.getSimpleName();
    
    // Since we want each test to start with a clean slate
    void deleteTheDatabase() {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    public void setUp() {
        deleteTheDatabase();
    }
    
    public void testCreateDb() throws Throwable {
        final HashSet<String> tableNameHashSet = new HashSet<String>();
    
        tableNameHashSet.add(WeatherContract.LocationEntry.TABLE_NAME);
        tableNameHashSet.add(WeatherContract.WeatherEntry.TABLE_NAME);
    
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
    
        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
        c.moveToFirst());

        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while( c.moveToNext() );

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without both the location entry and weather entry  tables",
        tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + WeatherContract.LocationEntry.TABLE_NAME + ")", null);
        
        assertTrue("Error: This means that we were unable to query the database for table information.",
        c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> locationColumnHashSet = new HashSet<String>();
        
        locationColumnHashSet.add(WeatherContract.LocationEntry._ID);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_CITY_NAME);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LAT);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_COORD_LONG);
        locationColumnHashSet.add(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            locationColumnHashSet.remove(columnName);
        } while(c.moveToNext());

        // if this fails, it means that your database doesn't contain all of the required location
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required location entry columns",
            locationColumnHashSet.isEmpty());
        db.close();
    }
    
    public long testLocationTable() {
        // First step: Get reference to writable database
        WeatherDbHelper helper = new WeatherDbHelper(getContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        
        // Create ContentValues of what you want to insert
        ContentValues contentValues = new ContentValues();
        contentValues.putAll(TestUtilities.createNorthPoleLocationValues());
        
        // (you can use the createNorthPoleLocationValues if you wish)
        // Insert ContentValues into database and get a row ID back
        long move = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, contentValues);
        assertTrue(move != -1);
        
        // Query the database and receive a Cursor back
        Cursor cursor = db.query(WeatherContract.LocationEntry.TABLE_NAME,
                null, null, null, null, null, null);
        assertTrue("ERROR: No Records at Location query", cursor.moveToFirst());
        TestUtilities.validateCurrentRecord("ERROR", cursor, contentValues);
        assertFalse("ERROR: MORE THAN ONE ENTRY at location query", cursor.moveToNext());
        
        cursor.close();
        helper.close();
        return move;
        // Move the cursor to a valid database row
        // Validate data in resulting Cursor with the original ContentValues
        // (you can use the validateCurrentRecord function in TestUtilities to validate the
        // query if you like)
        // Finally, close the cursor and database
    }
    
    public void testWeatherTable() {
        WeatherDbHelper helper = new WeatherDbHelper(getContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        long move = testLocationTable();
        ContentValues content = TestUtilities.createWeatherValues(move);
        long welp = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, content);
        
        assertTrue(welp != -1);
        
        Cursor curs = db.query(WeatherContract.WeatherEntry.TABLE_NAME,
                null, null, null, null, null, null);
        assertTrue("ERROR: NO RECORDS at Weather query", curs.moveToFirst());
        TestUtilities.validateCurrentRecord("ERROR", curs, content);
        
        curs.close();
        helper.close();
    }
}
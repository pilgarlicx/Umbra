/*******************************************************************************
 * This file is part of Umbra.
 * 
 *     Umbra is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     Umbra is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with Umbra.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *     Copyright (c) 2011 Vasile Jureschi <vasile.jureschi@gmail.com>.
 *     All rights reserved. This program and the accompanying materials
 *     are made available under the terms of the GNU Public License v3.0
 *     which accompanies this distribution, and is available at
 *     
 *    http://www.gnu.org/licenses/gpl-3.0.html
 * 
 *     Contributors:
 *        Vasile Jureschi <vasile.jureschi@gmail.com> - initial API and implementation
 ******************************************************************************/

package org.unchiujar.umbra;

import static org.unchiujar.umbra.LogUtilities.numberLogList;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class LocationRecorder implements LocationProvider {
    private static final String TAG = LocationRecorder.class.getName();
    private static final String DATABASE_NAME = "visited.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "coordinates";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private static final String DATABASE_PROVIDER = "Visited";

    private Context context;
    private SQLiteDatabase db;
    private SQLiteStatement insertStmt;
    private static final String INSERT = "insert into " + TABLE_NAME + "(" + LATITUDE + "," + LONGITUDE
            + ") values (?,?)";

    private static LocationRecorder instance;

    private LocationRecorder(Context context) {
        this.context = context;
        OpenHelper openHelper = new OpenHelper(this.context);
        this.db = openHelper.getWritableDatabase();
        // this.db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // openHelper.onCreate(db);
        this.insertStmt = this.db.compileStatement(INSERT);
        
    }

    public static LocationRecorder getInstance(Context context) {
        return (instance == null) ? instance = new LocationRecorder(context) : instance;
    }

    public long insert(AproximateLocation location) {

        this.insertStmt.bindDouble(1, location.getLatitude());
        this.insertStmt.bindDouble(2, location.getLongitude());
        long index = this.insertStmt.executeInsert();
        Log.d(TAG, DATABASE_NAME + "Inserted latitude and longitude: "
                + numberLogList(location.getLatitude(), location.getLongitude()));

        return index;
    }

    public void insert(List<AproximateLocation> locations) {
        DatabaseUtils.InsertHelper batchInserter = new DatabaseUtils.InsertHelper(db, TABLE_NAME);
        int latitudeIndex = batchInserter.getColumnIndex(LATITUDE);
        int longitudeIndex = batchInserter.getColumnIndex(LONGITUDE);
        
        // see http://notes.theorbis.net/2010/02/batch-insert-to-sqlite-on-android.html
        for (AproximateLocation aproximateLocation : locations) {
            batchInserter.prepareForInsert();
            batchInserter.bind(latitudeIndex, aproximateLocation.getLatitude());
            batchInserter.bind(longitudeIndex, aproximateLocation.getLongitude());
            batchInserter.execute();
            Log.d(TAG, "Batch inserted latitude and longitude: "
                    + numberLogList(aproximateLocation.getLatitude(), aproximateLocation.getLongitude()));

        }
        batchInserter.close();
    }

    public void deleteAll() {
        this.db.delete(TABLE_NAME, null, null);
    }

    public List<AproximateLocation> selectAll() {

        List<AproximateLocation> list = new ArrayList<AproximateLocation>();
        Cursor cursor = this.db.query(TABLE_NAME, new String[] { LATITUDE, LONGITUDE }, null, null, null,
                null, LONGITUDE + " desc");
        Log.d(TAG, "Results obtained: " + cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                AproximateLocation location = new AproximateLocation(DATABASE_PROVIDER);
                location.setLatitude(cursor.getDouble(0));
                location.setLongitude(cursor.getDouble(1));
                list.add(location);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return list;
    }

    public List<AproximateLocation> selectVisited(AproximateLocation upperLeft, AproximateLocation lowerRight) {

        List<AproximateLocation> list = new ArrayList<AproximateLocation>();
        double longitudeMin = upperLeft.getLongitude();
        double latitudeMax = upperLeft.getLatitude();
        double longitudeMax = lowerRight.getLongitude();
        double latitudeMin = lowerRight.getLatitude();

        String condition = LONGITUDE + " >= " + longitudeMin + " AND " + LONGITUDE + " <= " + longitudeMax
                + " AND " + LATITUDE + " >= " + latitudeMin + " AND " + LATITUDE + " <= " + latitudeMax;

        Log.v(TAG, "Select condition is " + condition);
        Cursor cursor = this.db.query(TABLE_NAME, new String[] { LATITUDE, LONGITUDE }, condition, null,
                null, null, LATITUDE + " desc");
        Log.d(TAG, "Results obtained: " + cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                AproximateLocation location = new AproximateLocation(DATABASE_PROVIDER);
                location.setLatitude(cursor.getDouble(0));
                location.setLongitude(cursor.getDouble(1));
                Log.v(TAG, "Added to list of results obtained: " + location);
                list.add(location);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    private static class OpenHelper extends SQLiteOpenHelper {

        OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "Creating database: " + TABLE_NAME);
            db.execSQL("CREATE TABLE " + TABLE_NAME + "(id INTEGER PRIMARY KEY, " + LATITUDE + " REAL, "
                    + LONGITUDE + " REAL)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database, this will drop tables and recreate.");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
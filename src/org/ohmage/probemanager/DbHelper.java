/*******************************************************************************
 * Copyright 2011 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package org.ohmage.probemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.DbContract.Responses;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "probes.db";
    private static final int DB_VERSION = 5;

    public interface Tables {
        static final String Probes = "probes";
        static final String Responses = "responses";
    }

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Probes + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Probes.OBSERVER_ID + " TEXT NOT NULL, "
                + Probes.OBSERVER_VERSION + " INTEGER NOT NULL, "
                + Probes.STREAM_ID + " TEXT NOT NULL, "
                + Probes.STREAM_VERSION + " INTEGER NOT NULL, "
                + Probes.UPLOAD_PRIORITY + " INTEGER DEFAULT 0, "
                + Probes.USERNAME + " TEXT NOT NULL, "
                + Probes.PROBE_METADATA + " TEXT, "
                + Probes.PROBE_DATA + " TEXT);");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Tables.Responses + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Responses.CAMPAIGN_URN + " TEXT NOT NULL, "
                + Responses.CAMPAIGN_CREATED + " TEXT NOT NULL, "
                + Responses.UPLOAD_PRIORITY + " INTEGER DEFAULT 0, "
                + Responses.USERNAME + " TEXT NOT NULL, "
                + Responses.RESPONSE_DATA + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Probes);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Responses);
        onCreate(db);
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Probes);
        db.execSQL("DROP TABLE IF EXISTS " + Tables.Responses);
        onCreate(db);
    }
}

/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/*
* Modifications:
* -Imported from AOSP frameworks/base/core/java/com/android/internal/content
* -Changed package name
*/

/*
 * Imported from android iosched2011 project:
 * https://github.com/underhilllabs/iosched2011/blob/master/android/src/com/google/android/apps/iosched/util/SelectionBuilder.java
 */
package org.ohmage.db.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

/**
* Helper for building selection clauses for {@link SQLiteDatabase}. Each
* appended clause is combined using {@code AND}. This class is <em>not</em>
* thread safe.
*/
public class SelectionBuilder {
    private static final String TAG = "SelectionBuilder";
    private static final boolean LOGV = true;

    private String mTable = null;
    private Map<String, String> mProjectionMap = Maps.newHashMap();
    private StringBuilder mSelection = new StringBuilder();
    private ArrayList<String> mSelectionArgs = Lists.newArrayList();
    private ArrayList<String> mJoins = Lists.newArrayList();

    /**
* Reset any internal state, allowing this builder to be recycled.
*/
    public SelectionBuilder reset() {
        mTable = null;
        mSelection.setLength(0);
        mSelectionArgs.clear();
        return this;
    }

    /**
* Append the given selection clause to the internal state. Each clause is
* surrounded with parenthesis and combined using {@code AND}.
*/
    public SelectionBuilder where(String selection, String... selectionArgs) {
        if (TextUtils.isEmpty(selection)) {
            if (selectionArgs != null && selectionArgs.length > 0) {
                throw new IllegalArgumentException(
                        "Valid selection required when including arguments=");
            }

            // Shortcut when clause is empty
            return this;
        }

        if (mSelection.length() > 0) {
            mSelection.append(" AND ");
        }

        mSelection.append("(").append(selection).append(")");
        if (selectionArgs != null) {
            for (String arg : selectionArgs) {
                mSelectionArgs.add(arg);
            }
        }

        return this;
    }

    public SelectionBuilder table(String table) {
        mTable = table;
        return this;
    }
    
    /**
     * Joins a table to the query; only works with query() calls, since sql officially doesn't support joins on an update/delete.
     * Calls to update() and delete() will ignore the joins.
     * 
     * Note that you must specify at least one clause. For convenience, any occurrences of the string "%t" are
     * replaced with the 'target' parameter, and any occurrences of string "%s" are replaced with the original table.
     * Only use "%s" if the original table is a single table name, of course, and not a join itself.
     * 
     * @param target the table to join to this query
     * @param clauses at least one constraint to apply to the join; multiple clauses are connected by 'and'
     * @return the SelectionBuilder used in the call, so that subsequent calls can be chained
     */
    public SelectionBuilder join(String target, String... clauses) {
    	assertTable();
    	
    	if (clauses.length <= 0)
    		throw new IllegalArgumentException("at least one clause must be specified in a join");
    	
    	// replace instances of %t with the target table and %s with the original table
    	for (int i = 0; i < clauses.length; ++i)
    		clauses[i] = clauses[i].replaceAll("%t", target).replaceAll("%s", mTable);
    	
    	mJoins.add("inner join " + target + " on " + arrayToString2(clauses, " AND "));
    	return this;
    }

    private void assertTable() {
        if (mTable == null) {
            throw new IllegalStateException("Table not specified");
        }
    }

    public SelectionBuilder mapToTable(String column, String table) {
        mProjectionMap.put(column, table + "." + column);
        return this;
    }

    public SelectionBuilder map(String fromColumn, String toClause) {
        mProjectionMap.put(fromColumn, toClause + " AS " + fromColumn);
        return this;
    }

    /**
* Return selection string for current internal state.
*
* @see #getSelectionArgs()
*/
    public String getSelection() {
        return mSelection.toString();
    }

    /**
* Return selection arguments for current internal state.
*
* @see #getSelection()
*/
    public String[] getSelectionArgs() {
        return mSelectionArgs.toArray(new String[mSelectionArgs.size()]);
    }

    private void mapColumns(String[] columns) {
        for (int i = 0; i < columns.length; i++) {
            final String target = mProjectionMap.get(columns[i]);
            if (target != null) {
                columns[i] = target;
            }
        }
    }

    @Override
    public String toString() {
        return "SelectionBuilder[table=" + mTable + ", selection=" + getSelection()
                + ", selectionArgs=" + Arrays.toString(getSelectionArgs()) + "]";
    }

    /**
* Execute query using the current internal state as {@code WHERE} clause.
*/
    public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
        return query(db, columns, null, null, orderBy, null);
    }

    /**
* Execute query using the current internal state as {@code WHERE} clause.
*/
    public Cursor query(SQLiteDatabase db, String[] columns, String groupBy,
            String having, String orderBy, String limit) {
        assertTable();
        if (columns != null) mapColumns(columns);
        
        // if there are joins, this is the table + any joins added on
        String compositeTable = mTable;
        
        // add on all the joins!
        for (String join : mJoins)
        	compositeTable += " " + join;
        
        if (LOGV) Log.v(TAG, "query(columns=" + Arrays.toString(columns) + ") " + this);
        return db.query(compositeTable, columns, getSelection(), getSelectionArgs(), groupBy, having,
                orderBy, limit);
    }

    /**
* Execute update using the current internal state as {@code WHERE} clause.
*/
    public int update(SQLiteDatabase db, ContentValues values) {
        assertTable();
        if (LOGV) Log.v(TAG, "update() " + this);
        return db.update(mTable, values, getSelection(), getSelectionArgs());
    }

    /**
* Execute delete using the current internal state as {@code WHERE} clause.
*/
    public int delete(SQLiteDatabase db) {
        assertTable();
        if (LOGV) Log.v(TAG, "delete() " + this);
        return db.delete(mTable, getSelection(), getSelectionArgs());
    }
    
    // utility method for gluing string arrays together
    private static String arrayToString2(String[] a, String separator) {
        StringBuffer result = new StringBuffer();
        if (a.length > 0) {
            result.append(a[0]);
            for (int i=1; i<a.length; i++) {
                result.append(separator);
                result.append(a[i]);
            }
        }
        return result.toString();
    }
}
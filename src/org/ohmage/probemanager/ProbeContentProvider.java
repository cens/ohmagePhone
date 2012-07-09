
package org.ohmage.probemanager;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.DbHelper.Tables;

public class ProbeContentProvider extends ContentProvider {

    // enum of the URIs we can match using sUriMatcher
    private interface MatcherTypes {
        int PROBES = 0;
    }

    private DbHelper dbHelper;
    private static UriMatcher sUriMatcher;
    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, "probes", MatcherTypes.PROBES);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.PROBES:
                return Probes.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("getType(): Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.PROBES:
                long id = dbHelper.getWritableDatabase().insert(Tables.Probes, BaseColumns._ID,
                        values);
                if (id != -1)
                    return ContentUris.withAppendedId(Probes.CONTENT_URI, id);
                break;
            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.PROBES:
                return dbHelper.getReadableDatabase().query(Tables.Probes, projection, selection,
                        selectionArgs, null, null, null);
            default:
                throw new UnsupportedOperationException("query(): Unknown URI: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int count = 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            switch (sUriMatcher.match(uri)) {
                case MatcherTypes.PROBES:
                    for (ContentValues v : values) {
                        if (db.insert(Tables.Probes, BaseColumns._ID, v) != -1)
                            count++;
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("bulkInsert(): Unknown URI: " + uri);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return count;
    }
}


package org.ohmage.probemanager;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.DbContract.Responses;
import org.ohmage.probemanager.DbHelper.Tables;

public class ProbeContentProvider extends ContentProvider {

    // enum of the URIs we can match using sUriMatcher
    private interface MatcherTypes {
        int PROBES = 0;
        int RESPONSES = 1;
    }

    private DbHelper dbHelper;
    private static UriMatcher sUriMatcher;
    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, "probes", MatcherTypes.PROBES);
        sUriMatcher.addURI(DbContract.CONTENT_AUTHORITY, "responses", MatcherTypes.RESPONSES);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.PROBES:
                count = dbHelper.getWritableDatabase().delete(Tables.Probes,
                        selection == null ? "1" : selection, selectionArgs);
                break;
            case MatcherTypes.RESPONSES:
                count = dbHelper.getWritableDatabase().delete(Tables.Responses,
                        selection == null ? "1" : selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }

        notifyInsert(uri, count);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.PROBES:
                return Probes.CONTENT_TYPE;
            case MatcherTypes.RESPONSES:
                return Responses.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("getType(): Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = -1;
        ContentResolver cr = getContext().getContentResolver();

        switch (sUriMatcher.match(uri)) {
            case MatcherTypes.PROBES:
                id = dbHelper.getWritableDatabase().insert(Tables.Probes, BaseColumns._ID, values);
                cr.notifyChange(Probes.CONTENT_URI, null);
                break;
            case MatcherTypes.RESPONSES:
                id = dbHelper.getWritableDatabase().insert(Tables.Responses, BaseColumns._ID,
                        values);
                cr.notifyChange(Responses.CONTENT_URI, null);
                break;
            default:
                throw new UnsupportedOperationException("insert(): Unknown URI: " + uri);
        }
        if (id != -1)
            return ContentUris.withAppendedId(Probes.CONTENT_URI, id);
        return null;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Cursor cursor;
        switch (sUriMatcher.match(uri)) {

            case MatcherTypes.PROBES:
                cursor = dbHelper.getReadableDatabase().query(Tables.Probes, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case MatcherTypes.RESPONSES:
                cursor = dbHelper.getReadableDatabase().query(Tables.Responses, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("query(): Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        int count = 0;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            String table;
            switch (sUriMatcher.match(uri)) {
                case MatcherTypes.PROBES:
                    table = Tables.Probes;
                    break;
                case MatcherTypes.RESPONSES:
                    table = Tables.Responses;
                    break;
                default:
                    throw new UnsupportedOperationException("bulkInsert(): Unknown URI: " + uri);
            }

            for (ContentValues v : values) {
                if (db.insert(table, BaseColumns._ID, v) != -1)
                    count++;
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        notifyInsert(uri, count);

        return count;
    }

    private void notifyInsert(Uri uri, Integer count) {
        if (count > 0) {
            ContentResolver cr = getContext().getContentResolver();

            // depending on the type of the thing deleted, we have to notify
            // potentially many URIs
            switch (sUriMatcher.match(uri)) {
                case MatcherTypes.PROBES:
                    cr.notifyChange(Probes.CONTENT_URI, null);
                    break;

                case MatcherTypes.RESPONSES:
                    cr.notifyChange(Responses.CONTENT_URI, null);
                    break;
            }
        }
    }
}


package org.ohmage.service;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.ConfigHelper;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.UploadResponse;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.probemanager.DbContract.BaseProbeColumns;
import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.DbContract.Responses;

public class ProbeUploadService extends WakefulIntentService {

    /** Extra to tell the upload service if it is running in the background */
    public static final String EXTRA_BACKGROUND = "is_background";

    private static final String TAG = "ProbeUploadService";

    public static final String PROBE_UPLOAD_STARTED = "org.ohmage.PROBE_UPLOAD_STARTED";
    public static final String PROBE_UPLOAD_FINISHED = "org.ohmage.PROBE_UPLOAD_FINISHED";
    public static final String PROBE_UPLOAD_ERROR = "org.ohmage.PROBE_UPLOAD_ERROR";

    public static final String RESPONSE_UPLOAD_STARTED = "org.ohmage.RESPONSE_UPLOAD_STARTED";
    public static final String RESPONSE_UPLOAD_FINISHED = "org.ohmage.RESPONSE_UPLOAD_FINISHED";
    public static final String RESPONSE_UPLOAD_ERROR = "org.ohmage.RESPONSE_UPLOAD_ERROR";

    private OhmageApi mApi;

    private boolean isBackground;

    private UserPreferencesHelper mUserPrefs;

    public ProbeUploadService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Analytics.service(this, Status.ON);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Analytics.service(this, Status.OFF);
    }

    @Override
    protected void doWakefulWork(Intent intent) {

        mUserPrefs = new UserPreferencesHelper(ProbeUploadService.this);

        if (mApi == null)
            setOhmageApi(new OhmageApi(this));

        isBackground = intent.getBooleanExtra(EXTRA_BACKGROUND, false);

        new ProbesUploader().upload();
        new ResponsesUploader().upload();
    }

    public void setOhmageApi(OhmageApi api) {
        mApi = api;
    }

    /**
     * Abstraction to upload object from the probes db. Uploads data in chunks
     * based on the {@link #getName(Cursor)} and {@link #getVersion(Cursor) values.
     * @author cketcham
     *
     */
    public abstract class Uploader {

        protected abstract Uri getContentURI();

        protected abstract UploadResponse uploadCall(String serverUrl, String username,
                String password, String client, Cursor c, JSONArray data);

        protected abstract void uploadStarted();

        protected abstract void uploadFinished();

        protected abstract void uploadError();

        protected abstract void addProbe(JSONArray probes, Cursor c);

        protected abstract int getVersionColumn();

        protected abstract int getNameColumn();

        protected abstract String[] getProjection();

        public void upload() {

            uploadStarted();

            Cursor c = getContentResolver().query(getContentURI(), getProjection(),
                    BaseProbeColumns.USERNAME + "=?", new String[] {
                        mUserPrefs.getUsername()
                    }, getNameColumn() + ", " + getVersionColumn());

            int id_idx = c.getColumnIndex(BaseColumns._ID);

            JSONArray probes = new JSONArray();
            long maxId = 0;
            long nextMax = 0;

            for (int i = 0; i < c.getCount(); i++) {
                c.moveToPosition(i);
                addProbe(probes, c);
                nextMax = Math.max(c.getLong(id_idx), nextMax);

                String observerId = c.getString(getNameColumn());
                String observerVersion = c.getString(getVersionColumn());

                // Only move to the next one if we aren't at the last one
                if(!c.isLast())
                    c.moveToNext();

                // Upload if we have no more points, we already have 100 points
                // defined or the next point is from a different observer
                if (c.isLast() || i % 100 == 0
                        || (!observerId.equals(c.getString(getNameColumn()))
                        || !observerVersion.equals(c.getString(getVersionColumn())))) {
                    // Try to upload. If it is an HTTP error, we stop uploading
                    if (!upload(probes, c))
                        break;
                    else
                        maxId = nextMax;
                }
            }

            c.close();
            getContentResolver().delete(getContentURI(),
                    BaseColumns._ID + "<=" + maxId + " AND " + BaseProbeColumns.USERNAME + "=?",
                    new String[] {
                        mUserPrefs.getUsername()
                    });
            uploadFinished();
        }

        /**
         * Uploads probes to the server
         * 
         * @param probes the probe json
         * @param c the cursor object
         * @return false only if there was an HTTP error indicating we shouldn't
         *         continue to try uploading
         */
        private boolean upload(JSONArray probes, Cursor c) {

            String username = mUserPrefs.getUsername();
            String hashedPassword = mUserPrefs.getHashedPassword();

            if (probes.length() > 0) {
                UploadResponse response = uploadCall(ConfigHelper.serverUrl(), username,
                        hashedPassword, OhmageApi.CLIENT_NAME, c, probes);
                response.handleError(ProbeUploadService.this);

                if (response.getResult().equals(OhmageApi.Result.SUCCESS)) {
                    NotificationHelper.hideMobilityErrorNotification(ProbeUploadService.this);
                } else {
                    if (isBackground && !response.hasAuthError()
                            && !response.getResult().equals(OhmageApi.Result.HTTP_ERROR))
                        NotificationHelper.showMobilityErrorNotification(ProbeUploadService.this);
                }
                probes = new JSONArray();
                if(response.getResult().equals(OhmageApi.Result.HTTP_ERROR)) {
                    return false;
                }
            }
            return true;
        }
    }

    private interface ProbeQuery {
        static final String[] PROJECTION = new String[] {
                Probes._ID, Probes.OBSERVER_ID, Probes.OBSERVER_VERSION, Probes.STREAM_ID,
                Probes.STREAM_VERSION, Probes.PROBE_METADATA, Probes.PROBE_DATA
        };

        static final int OBSERVER_ID = 1;
        static final int OBSERVER_VERSION = 2;
        static final int STREAM_ID = 3;
        static final int STREAM_VERSION = 4;
        static final int PROBE_METADATA = 5;
        static final int PROBE_DATA = 6;
    }

    public class ProbesUploader extends Uploader {

        protected String[] getProjection() {
            return ProbeQuery.PROJECTION;
        }

        protected int getNameColumn() {
            return ProbeQuery.OBSERVER_ID;
        }

        protected int getVersionColumn() {
            return ProbeQuery.OBSERVER_VERSION;
        }

        public void addProbe(JSONArray probes, Cursor c) {
            try {
                JSONObject probe = new JSONObject();
                probe.put("stream_id", c.getString(ProbeQuery.STREAM_ID));
                probe.put("stream_version", c.getInt(ProbeQuery.STREAM_VERSION));
                probe.put("data", new JSONObject(c.getString(ProbeQuery.PROBE_DATA)));
                String metadata = c.getString(ProbeQuery.PROBE_METADATA);
                if (!TextUtils.isEmpty(metadata))
                    probe.put("metadata", new JSONObject(metadata));
                probes.put(probe);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        protected void uploadStarted() {
            sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_STARTED));
        }

        @Override
        protected void uploadFinished() {
            sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_FINISHED));
        }

        @Override
        protected void uploadError() {
            sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_ERROR));
        }

        @Override
        protected Uri getContentURI() {
            return Probes.CONTENT_URI;
        }

        @Override
        protected UploadResponse uploadCall(String serverUrl, String username, String password,
                String client, Cursor c, JSONArray data) {
            return mApi.observerUpload(ConfigHelper.serverUrl(), username,
                    password, OhmageApi.CLIENT_NAME, c.getString(ProbeQuery.OBSERVER_ID),
                    c.getString(ProbeQuery.OBSERVER_VERSION), data.toString());
        }
    }

    private interface ResponseQuery {
        static final String[] PROJECTION = new String[] {
                Responses._ID, Responses.CAMPAIGN_URN, Responses.CAMPAIGN_CREATED,
                Responses.RESPONSE_DATA
        };

        static final int CAMPAIGN_URN = 1;
        static final int CAMPAIGN_CREATED = 2;
        static final int RESPONSE_DATA = 3;
    }

    public class ResponsesUploader extends Uploader {

        protected String[] getProjection() {
            return ResponseQuery.PROJECTION;
        }

        protected int getNameColumn() {
            return ResponseQuery.CAMPAIGN_URN;
        }

        protected int getVersionColumn() {
            return ResponseQuery.CAMPAIGN_CREATED;
        }

        public void addProbe(JSONArray probes, Cursor c) {
            try {
                JSONObject response = new JSONObject(c.getString(ResponseQuery.RESPONSE_DATA));
                probes.put(response);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        protected void uploadStarted() {
            sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_STARTED));
        }

        @Override
        protected void uploadFinished() {
            sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_FINISHED));
        }

        @Override
        protected void uploadError() {
            sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_ERROR));
        }

        @Override
        protected Uri getContentURI() {
            return Responses.CONTENT_URI;
        }

        @Override
        protected UploadResponse uploadCall(String serverUrl, String username, String password,
                String client, Cursor c, JSONArray data) {
            return mApi.surveyUpload(ConfigHelper.serverUrl(), username,
                    password, OhmageApi.CLIENT_NAME, c.getString(ResponseQuery.CAMPAIGN_URN),
                    c.getString(ResponseQuery.CAMPAIGN_CREATED), data.toString());
        }
    }
}


package org.ohmage.service;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;

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

    private static final int BATCH_SIZE = 500;

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
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
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

        protected JsonParser mParser;

        public Uploader() {
            mParser = new JsonParser();
        }

        protected abstract Uri getContentURI();

        protected abstract UploadResponse uploadCall(String serverUrl, String username,
                String password, String client, String name, String version, JsonArray data);

        protected abstract void uploadStarted();

        protected abstract void uploadFinished();

        protected abstract void uploadError();

        protected abstract void addProbe(JsonArray probes, Cursor c);

        protected abstract int getVersionIndex();

        protected abstract int getNameIndex();

        protected abstract String getVersionColumn();

        protected abstract String getNameColumn();

        protected abstract String[] getProjection();

        public void upload() {

            uploadStarted();

            Cursor c = getContentResolver().query(getContentURI(), getProjection(),
                    BaseProbeColumns.USERNAME + "=?", new String[] {
                        mUserPrefs.getUsername()
                    }, getNameColumn() + ", " + getVersionColumn());

            JsonArray probes = new JsonArray();

            String currentObserver = null;
            String currentVersion = null;
            String observerId = null;
            String observerVersion = null;

            if (c.moveToFirst()) {
                currentObserver = c.getString(getNameIndex());
                currentVersion = c.getString(getVersionIndex());
            }

            for (int i = 0; i < c.getCount() + 1; i++) {

                c.moveToPosition(i);

                if (!c.isAfterLast()) {
                    observerId = c.getString(getNameIndex());
                    observerVersion = c.getString(getVersionIndex());
                }

                // If we have a batch or we see a different point, upload all
                // the points we have so far
                if (probes.size() % BATCH_SIZE == 0
                        || c.isAfterLast()
                        || (!observerId.equals(currentObserver) || !observerVersion
                                .equals(currentVersion))) {

                    if (!upload(probes, currentObserver, currentVersion)) {
                        c.close();
                        uploadError();
                        return;
                    }

                    if (c.isAfterLast())
                        break;

                    probes = new JsonArray();
                }

                currentObserver = observerId;
                currentVersion = observerVersion;

                addProbe(probes, c);
            }

            // Deleting all points since it is very slow to delete in batches if
            // there are lots of points
            getContentResolver().delete(getContentURI(), BaseProbeColumns.USERNAME + "=?",
                    new String[] {
                        mUserPrefs.getUsername()
                    });

            c.close();
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
        private boolean upload(JsonArray probes, String observerId, String observerVersion) {

            String username = mUserPrefs.getUsername();
            String hashedPassword = mUserPrefs.getHashedPassword();

            if (probes.size() > 0) {
                UploadResponse response = uploadCall(ConfigHelper.serverUrl(), username,
                        hashedPassword, OhmageApi.CLIENT_NAME, observerId, observerVersion, probes);
                response.handleError(ProbeUploadService.this);

                if (response.getResult().equals(OhmageApi.Result.SUCCESS)) {
                    NotificationHelper.hideMobilityErrorNotification(ProbeUploadService.this);
                } else {
                    if (isBackground && !response.hasAuthError()
                            && !response.getResult().equals(OhmageApi.Result.HTTP_ERROR))
                        NotificationHelper.showMobilityErrorNotification(ProbeUploadService.this);
                }
                if (response.getResult().equals(OhmageApi.Result.HTTP_ERROR)) {
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

        @Override
        protected String[] getProjection() {
            return ProbeQuery.PROJECTION;
        }

        @Override
        protected int getNameIndex() {
            return ProbeQuery.OBSERVER_ID;
        }

        @Override
        protected int getVersionIndex() {
            return ProbeQuery.OBSERVER_VERSION;
        }

        @Override
        public void addProbe(JsonArray probes, Cursor c) {
            JsonObject probe = new JsonObject();
            probe.addProperty("stream_id", c.getString(ProbeQuery.STREAM_ID));
            probe.addProperty("stream_version", c.getInt(ProbeQuery.STREAM_VERSION));
            probe.add("data", mParser.parse(c.getString(ProbeQuery.PROBE_DATA)));
            String metadata = c.getString(ProbeQuery.PROBE_METADATA);
            if (!TextUtils.isEmpty(metadata))
                probe.add("metadata", mParser.parse(metadata));
            probes.add(probe);
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
                String client, String observerId, String observerVersion, JsonArray data) {
            return mApi.observerUpload(ConfigHelper.serverUrl(), username,
                    password, OhmageApi.CLIENT_NAME, observerId, observerVersion, data.toString());
        }

        @Override
        protected String getVersionColumn() {
            return Probes.OBSERVER_VERSION;
        }

        @Override
        protected String getNameColumn() {
            return Probes.OBSERVER_ID;
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

        @Override
        protected String[] getProjection() {
            return ResponseQuery.PROJECTION;
        }

        @Override
        protected int getNameIndex() {
            return ResponseQuery.CAMPAIGN_URN;
        }

        @Override
        protected int getVersionIndex() {
            return ResponseQuery.CAMPAIGN_CREATED;
        }

        @Override
        public void addProbe(JsonArray probes, Cursor c) {
            probes.add(mParser.parse(c.getString(ResponseQuery.RESPONSE_DATA)));
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
                String client, String campaignUrn, String campaignCreated, JsonArray data) {
            return mApi.surveyUpload(ConfigHelper.serverUrl(), username,
                    password, OhmageApi.CLIENT_NAME, campaignUrn, campaignCreated, data.toString());
        }

        @Override
        protected String getVersionColumn() {
            return Responses.CAMPAIGN_URN;
        }

        @Override
        protected String getNameColumn() {
            return Responses.CAMPAIGN_CREATED;
        }
    }
}

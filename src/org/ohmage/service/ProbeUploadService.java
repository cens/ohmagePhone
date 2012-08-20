
package org.ohmage.service;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.ucla.cens.systemlog.Analytics;
import edu.ucla.cens.systemlog.Analytics.Status;
import edu.ucla.cens.systemlog.Log;

import org.ohmage.ConfigHelper;
import org.ohmage.NotificationHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.UploadResponse;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.probemanager.DbContract.BaseProbeColumns;
import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.DbContract.Responses;

import java.util.ArrayList;

public class ProbeUploadService extends WakefulIntentService {

    /** Extra to tell the upload service if it is running in the background **/
    public static final String EXTRA_BACKGROUND = "is_background";

    /** Uploaded in batches of 0.5 mb */
    private static final int BATCH_SIZE = 1024 * 1024 / 2;

    private static final String TAG = "ProbeUploadService";

    public static final String PROBE_UPLOAD_STARTED = "org.ohmage.PROBE_UPLOAD_STARTED";
    public static final String PROBE_UPLOAD_FINISHED = "org.ohmage.PROBE_UPLOAD_FINISHED";
    public static final String PROBE_UPLOAD_ERROR = "org.ohmage.PROBE_UPLOAD_ERROR";

    public static final String RESPONSE_UPLOAD_STARTED = "org.ohmage.RESPONSE_UPLOAD_STARTED";
    public static final String RESPONSE_UPLOAD_FINISHED = "org.ohmage.RESPONSE_UPLOAD_FINISHED";
    public static final String RESPONSE_UPLOAD_ERROR = "org.ohmage.RESPONSE_UPLOAD_ERROR";

    public static final String PROBE_UPLOAD_SERVICE_FINISHED = "org.ohmage.PROBE_UPLOAD_SERVICE_FINISHED";

    public static final String EXTRA_PROBE_ERRORS = "extra_probe_errors";

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

        Log.d(TAG, "upload probes");
        ProbesUploader probesUploader = new ProbesUploader();
        probesUploader.upload();
        Log.d(TAG, "upload responses");
        ResponsesUploader responsesUploader = new ResponsesUploader();
        responsesUploader.upload();

        // If there were no internal errors, we can say it was successful
        if (!probesUploader.hadError() && !responsesUploader.hadError())
            mUserPrefs.putLastProbeUploadTimestamp(System.currentTimeMillis());

        sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_SERVICE_FINISHED));
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
        protected ArrayList<String> mErrors = new ArrayList<String>();

        public Uploader() {
            mParser = new JsonParser();
        }

        protected abstract Uri getContentURI();

        protected abstract UploadResponse uploadCall(String serverUrl, String username,
                String password, String client, String name, String version, JsonArray data);

        protected abstract void uploadStarted();

        protected abstract void uploadBatchSuccess();

        protected abstract void uploadFinished();

        protected abstract void uploadError();

        /**
         * Adds a probe to the json array
         * 
         * @param probes
         * @param c
         * @return the number of bytes in the payload
         */
        protected abstract int addProbe(JsonArray probes, Cursor c);

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

            StringBuilder delete = new StringBuilder();

            int payloadSize = 0;

            if (c.moveToFirst()) {
                currentObserver = c.getString(getNameIndex());
                currentVersion = c.getString(getVersionIndex());
            }

            for (int i = 0; i < c.getCount() + 1; i++) {

                try {
                    c.moveToPosition(i);
                } catch (IllegalStateException e) {
                    // Due to a bug in 4.0 and greater(?) a crash can occur
                    // during the move.
                    // There is no good way to recover so we just restart
                    // More info here:
                    // http://code.google.com/p/android/issues/detail?id=32472
                    Log.e(TAG, "illegal state exception moving to " + i + " of "
                            + (c.getCount() + 1));
                    // Lets restart!
                    upload();
                    return;
                }

                if (!c.isAfterLast()) {
                    observerId = c.getString(getNameIndex());
                    observerVersion = c.getString(getVersionIndex());
                }

                // If we have a batch or we see a different point, upload all
                // the points we have so far
                if (payloadSize > BATCH_SIZE
                        || c.isAfterLast()
                        || (!observerId.equals(currentObserver) || !observerVersion
                                .equals(currentVersion))) {
                    Log.d(TAG, "total payload for " + currentObserver + "=" + payloadSize);
                    if (!upload(probes, currentObserver, currentVersion)) {
                        c.close();
                        uploadError();
                        return;
                    }

                    // Deleting this batch of points
                    getContentResolver().delete(getContentURI(), delete.toString(), null);
                    delete = new StringBuilder();

                    if (c.isAfterLast())
                        break;

                    payloadSize = 0;
                    probes = new JsonArray();
                }

                currentObserver = observerId;
                currentVersion = observerVersion;

                payloadSize += addProbe(probes, c);
                if (delete.length() != 0)
                    delete.append(" OR ");
                delete.append(BaseColumns._ID + "=" + c.getLong(0));
            }

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
                    uploadBatchSuccess();
                } else {
                    if (!response.hasAuthError()
                            && !response.getResult().equals(OhmageApi.Result.HTTP_ERROR)) {
                        mErrors.add(observerId + response.getErrorCodes().toString());
                        uploadError();
                    }
                }
                if (response.getResult().equals(OhmageApi.Result.HTTP_ERROR)) {
                    return false;
                }
            }
            return true;
        }

        public boolean hadError() {
            return !mErrors.isEmpty();
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
        public int addProbe(JsonArray probes, Cursor c) {
            JsonObject probe = new JsonObject();
            probe.addProperty("stream_id", c.getString(ProbeQuery.STREAM_ID));
            probe.addProperty("stream_version", c.getInt(ProbeQuery.STREAM_VERSION));
            String data = c.getString(ProbeQuery.PROBE_DATA);
            int size = 0;
            if (!TextUtils.isEmpty(data)) {
                size += data.getBytes().length;
                probe.add("data", mParser.parse(data));
            }
            String metadata = c.getString(ProbeQuery.PROBE_METADATA);
            if (!TextUtils.isEmpty(metadata)) {
                size += metadata.getBytes().length;
                probe.add("metadata", mParser.parse(metadata));
            }
            probes.add(probe);
            return size;
        }

        @Override
        protected void uploadStarted() {
            sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_STARTED));
        }

        @Override
        protected void uploadBatchSuccess() {
            NotificationHelper.hideProbeUploadErrorNotification(ProbeUploadService.this);
        }

        @Override
        protected void uploadFinished() {
            sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_FINISHED));
        }

        @Override
        protected void uploadError() {
            if(isBackground)
                NotificationHelper.showProbeUploadErrorNotification(ProbeUploadService.this, mErrors);
            else
                sendBroadcast(new Intent(ProbeUploadService.PROBE_UPLOAD_ERROR).putExtra(EXTRA_PROBE_ERRORS, mErrors));
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
        public int addProbe(JsonArray probes, Cursor c) {
            String data = c.getString(ResponseQuery.RESPONSE_DATA);
            int size = 0;
            if (!TextUtils.isEmpty(data)) {
                size += data.getBytes().length;
                probes.add(mParser.parse(data));
            }
            return size;
        }

        @Override
        protected void uploadStarted() {
            sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_STARTED));
        }

        @Override
        protected void uploadBatchSuccess() {
            NotificationHelper.hideResponseUploadErrorNotification(ProbeUploadService.this);
        }

        @Override
        protected void uploadFinished() {
            sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_FINISHED));
        }

        @Override
        protected void uploadError() {
            if(isBackground)
                NotificationHelper.showResponseUploadErrorNotification(ProbeUploadService.this, mErrors);
            else
                sendBroadcast(new Intent(ProbeUploadService.RESPONSE_UPLOAD_ERROR).putExtra(EXTRA_PROBE_ERRORS, mErrors));
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

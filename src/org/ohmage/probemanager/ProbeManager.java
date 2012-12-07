
package org.ohmage.probemanager;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;

import org.ohmage.UserPreferencesHelper;
import org.ohmage.probemanager.DbContract.Probes;
import org.ohmage.probemanager.DbContract.Responses;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ProbeManager extends Service {

    /**
     * Set to true if we want to buffer points
     */
    private static final boolean BUFFER_POINTS = true;

    /**
     * buffer 25% of available memory, up to a maximum of 16MB
     */
    private static final long MAX_BYTE_SIZE = Math.min(Runtime.getRuntime().maxMemory() / 4,
            16 * 1024 * 1024);

    /**
     * Maximum number of points which should be in the buffer at any give time
     */
    private static final int MAX_BUFFER = 600;

    /**
     * Maximum number of milliseconds to wait before flushing data to db
     */
    private static final long FLUSH_DELAY = 300;

    /**
     * Number of bytes allocated to the data portions of probePoints
     */
    private int probeByteSize = 0;

    /**
     * Number of bytes allocated to the data portion of responsePoints
     */
    private int responseByteSize = 0;

    /**
     * Probe data
     */
    ArrayList<ContentValues> probePoints = new ArrayList<ContentValues>();

    /**
     * Response data
     */
    ArrayList<ContentValues> responsePoints = new ArrayList<ContentValues>();

    /**
     * Handles a flush message
     * 
     * @author cketcham
     */
    static class PointFlushHandler extends Handler {
        private final WeakReference<ProbeManager> mService;

        PointFlushHandler(ProbeManager service) {
            mService = new WeakReference<ProbeManager>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            ProbeManager service = mService.get();
            if (service != null) {
                service.flushProbes();
                service.flushResponses();
            }
        }
    }

    PointFlushHandler mHandler = new PointFlushHandler(this);
    private UserPreferencesHelper mUserPrefs;

    @Override
    public IBinder onBind(Intent intent) {
        return new IProbeManager.Stub() {

            @Override
            public boolean writeProbe(String observerId, int observerVersion, String streamId,
                    int streamVersion, int uploadPriority, String metadata, String data)
                    throws RemoteException {
                // Don't write a probe unless a user is logged into ohmage
                if (TextUtils.isEmpty(mUserPrefs.getUsername())) {
                    return false;
                }

                ContentValues values = new ContentValues();
                values.put(Probes.OBSERVER_ID, observerId);
                values.put(Probes.OBSERVER_VERSION, observerVersion);
                values.put(Probes.STREAM_ID, streamId);
                values.put(Probes.STREAM_VERSION, streamVersion);
                values.put(Probes.UPLOAD_PRIORITY, uploadPriority);
                values.put(Probes.PROBE_METADATA, metadata);
                values.put(Probes.PROBE_DATA, data);
                values.put(Probes.USERNAME, mUserPrefs.getUsername());

                if (BUFFER_POINTS) {
                    synchronized (probePoints) {
                        if(data != null)
                            probeByteSize += data.length();
                        if(metadata != null)
                            probeByteSize += metadata.length();
                        probePoints.add(values);
                        if (probePoints.size() > MAX_BUFFER
                                || probeByteSize + responseByteSize > MAX_BYTE_SIZE) {
                            mHandler.removeMessages(0);
                            flushProbes();
                        } else
                            queueFlush();
                    }
                    return true;
                } else {
                    return getContentResolver().insert(Probes.CONTENT_URI, values) != null;
                }
            }

            @Override
            public boolean writeResponse(String campaignUrn, String campaignCreationTimestamp,
                    int uploadPriority, String data) throws RemoteException {
                // Don't write a response unless a user is logged into ohmage
                if (TextUtils.isEmpty(mUserPrefs.getUsername()))
                    return false;

                ContentValues values = new ContentValues();
                values.put(Responses.CAMPAIGN_URN, campaignUrn);
                values.put(Responses.CAMPAIGN_CREATED, campaignCreationTimestamp);
                values.put(Responses.UPLOAD_PRIORITY, uploadPriority);
                values.put(Responses.RESPONSE_DATA, data);
                values.put(Responses.USERNAME, mUserPrefs.getUsername());

                if (BUFFER_POINTS) {
                    synchronized (responsePoints) {
                        if(data != null)
                            responseByteSize += data.length();
                        responsePoints.add(values);
                        if (responsePoints.size() > MAX_BUFFER
                                || probeByteSize + responseByteSize > MAX_BYTE_SIZE) {
                            mHandler.removeMessages(0);
                            flushResponses();
                        } else
                            queueFlush();
                    }
                    return true;
                } else {
                    return getContentResolver().insert(Responses.CONTENT_URI, values) != null;
                }
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mUserPrefs = new UserPreferencesHelper(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        flushProbes();
        flushResponses();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        flushProbes();
        flushResponses();
    }

    private void flushProbes() {
        ContentValues[] toFlush;
        synchronized (probePoints) {
            toFlush = probePoints.toArray(new ContentValues[] {});
            probePoints.clear();
            probeByteSize = 0;
        }
        getContentResolver().bulkInsert(Probes.CONTENT_URI, toFlush);
    }

    private void flushResponses() {
        ContentValues[] toFlush;
        synchronized (responsePoints) {
            toFlush = responsePoints.toArray(new ContentValues[] {});
            responsePoints.clear();
            responseByteSize = 0;
        }
        getContentResolver().bulkInsert(Responses.CONTENT_URI, toFlush);
    }

    private void queueFlush() {
        mHandler.removeMessages(0);
        mHandler.sendEmptyMessageDelayed(0, FLUSH_DELAY);
    }
}

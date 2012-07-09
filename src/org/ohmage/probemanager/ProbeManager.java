
package org.ohmage.probemanager;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import org.ohmage.probemanager.DbContract.Probes;

public class ProbeManager extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return new IProbeManager.Stub() {

            @Override
            public boolean send(String observerId, int observerVersion, String streamId,
                    int streamVersion, int uploadPriority, String metadata, String data)
                    throws RemoteException {
                ContentValues values = new ContentValues();
                values.put(Probes.OBSERVER_ID, observerId);
                values.put(Probes.OBSERVER_VERSION, observerVersion);
                values.put(Probes.STREAM_ID, streamId);
                values.put(Probes.STREAM_VERSION, streamVersion);
                values.put(Probes.UPLOAD_PRIORITY, uploadPriority);
                values.put(Probes.PROBE_METADATA, metadata);
                values.put(Probes.PROBE_DATA, data);
                return getContentResolver().insert(Probes.CONTENT_URI, values) != null;
            }
        };
    }
}

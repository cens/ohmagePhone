
package org.ohmage.probemanager;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for interacting with {@link ProbeContentProvider}. Defines the
 * kinds of entities managed by the provider, their schemas, and their
 * relationships.
 * 
 * @author cketcham
 */
public class DbContract {
    public static final String CONTENT_AUTHORITY = "org.ohmage.probemanager";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    interface ProbeColumns {
        /** Unique string identifying the observer */
        String OBSERVER_ID = "observer_id";
        /** Version to identify observer */
        String OBSERVER_VERSION = "observer_version";
        /** Unique string identifying the stream */
        String STREAM_ID = "stream_id";
        /** Version to identify stream */
        String STREAM_VERSION = "stream_version";
        /** Upload priority */
        String UPLOAD_PRIORITY = "upload_priority";
        /** Probe metadata */
        String PROBE_METADATA = "probe_metadata";
        /** Probe Data */
        String PROBE_DATA = "probe_data";
    }

    private static final String PATH_PROBES = "probes";

    /**
     * Represents a probe.
     */
    public static final class Probes implements BaseColumns, ProbeColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PROBES)
                .build();
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ohmage.probe";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ohmage.probe";

    }

    interface ResponseColumns {
        /** Unique string identifying the observer */
        String CAMPAIGN_URN = "campaign_urn";
        /** Version to identify observer */
        String CAMPAIGN_CREATED = "campaign_created";
        /** Probe Data */
        String RESPONSE_DATA = "response_data";
    }

    private static final String PATH_RESPONSES = "responses";

    /**
     * Represents a response submitted from a remote apk
     */
    public static final class Responses implements BaseColumns, ResponseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_RESPONSES)
                .build();
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ohmage.response";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ohmage.response";

    }
}

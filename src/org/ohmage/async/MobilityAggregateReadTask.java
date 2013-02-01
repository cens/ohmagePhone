
package org.ohmage.async;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;


import org.codehaus.jackson.JsonNode;
import org.ohmage.ConfigHelper;
import org.ohmage.MobilityHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.Response;
import org.ohmage.OhmageApi.Result;
import org.ohmage.OhmageApi.StreamingResponseListener;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.logprobe.Log;
import org.ohmage.mobility.glue.MobilityInterface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Loads Aggregate data about mobility from the server and populates the
 * mobility db
 */
public class MobilityAggregateReadTask extends AuthenticatedTaskLoader<Response> {

	private static final String TAG = "MobilityAggregateReadTask";
	private OhmageApi mApi;
	private final Context mContext;
	private final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
	private final Date mStartDate;
	private final Date mEndDate;
	private final UserPreferencesHelper mPrefs;

	public MobilityAggregateReadTask(final Context context) {
		super(context);
		mContext = context;
		mStartDate = new Date(UserPreferencesHelper.getBaseLineStartTime(mContext));
		Calendar now = Calendar.getInstance();
		now.add(Calendar.DATE, 1);
		mEndDate = now.getTime();

		mPrefs = new UserPreferencesHelper(mContext);
	}

	@Override
	public Response loadInBackground() {
		if (mApi == null)
			mApi = new OhmageApi(mContext);

		operations.clear();

		// First delete all the aggregate data we have so far
		operations.add(ContentProviderOperation.newDelete(MobilityInterface.AGGREGATES_URI)
				.withSelection(MobilityInterface.KEY_USERNAME + "=?",
						new String[] { MobilityHelper.getMobilityUsername(getUsername()) }).build());

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setLenient(false);

		Response response = mApi.mobilityAggregateRead(ConfigHelper.serverUrl(), getUsername(),
				getHashedPassword(), OhmageApi.CLIENT_NAME, sdf.format(mStartDate), sdf.format(mEndDate),
				1, new StreamingResponseListener() {

			@Override
			public void readObject(JsonNode object) {

				JsonNode data = object.get("data");

				for(int i=0;i<data.size();i++) {
					ContentValues values = new ContentValues();
					values.put(MobilityInterface.KEY_DURATION, data.get(i).get("duration").asLong(0));
					values.put(MobilityInterface.KEY_MODE, data.get(i).get("mode").asText());
					values.put(MobilityInterface.KEY_DAY, object.get("timestamp").asText());
					values.put(MobilityInterface.KEY_USERNAME, MobilityHelper.getMobilityUsername(getUsername()));
					operations.add(ContentProviderOperation.newInsert(MobilityInterface.AGGREGATES_URI).withValues(values).build());
				}
			}
		});

		// If the call was successful, we can update the aggregate table
		if(response.getResult() == Result.SUCCESS) {
			try {

				if(!mPrefs.isAuthenticated()) {
					Log.e(TAG, "User isn't logged in, terminating task");
					return response;
				}

				getContext().getContentResolver().applyBatch(MobilityInterface.AUTHORITY, operations);

				// Since we could loose the mobility timestamp by logging out, we do a sanity check
				UserPreferencesHelper sharedPrefs = new UserPreferencesHelper(mContext);
				Long mobilityTimestamp = sharedPrefs.getLastProbeUploadTimestamp();
				if(mobilityTimestamp == 0)
					mobilityTimestamp = sharedPrefs.getLoginTimestamp();
				if(mobilityTimestamp != 0 || operations.isEmpty())
					MobilityHelper.recalculateAggregate(mContext, getUsername(), mobilityTimestamp);

				mContext.getContentResolver().notifyChange(MobilityInterface.AGGREGATES_URI, null);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (OperationApplicationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// Thrown when mobility is uninstalled.
				e.printStackTrace();
			}
		}
		return response;
	}

	public void setOhmageApi(OhmageApi api) {
		mApi = api;
	}
}

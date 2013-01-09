package org.ohmage.authenticator;

import org.ohmage.ConfigHelper;
import org.ohmage.OhmageApi;
import org.ohmage.OhmageApi.AuthenticateResponse;
import org.ohmage.Utilities;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * Provides utility methods for communicating with the server.
 */
public class AuthenticationUtilities {
    private static final String TAG = "AuthenticationUtilities";

    /**
     * Connects to the Voiper server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean authenticate(String username, String password,
            Handler handler, final Context context) {

        AuthenticateResponse response = authenticate(username, password);

        sendResult(response, handler, context);
        if (response.getResult() == OhmageApi.Result.SUCCESS) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Successful authentication");
            }
            return true;
        } else {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Error authenticating" + response.getErrorCodes());
            }
            return false;
        }
    }

    public static OhmageApi.AuthenticateResponse authenticate(String username, String password) {
        OhmageApi api = new OhmageApi();
        return api.authenticate(ConfigHelper.serverUrl(), username, password, OhmageApi.CLIENT_NAME);
    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param result The boolean holding authentication result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     */
    private static void sendResult(final OhmageApi.AuthenticateResponse result, final Handler handler,
            final Context context) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                ((AuthenticatorActivity) context).onAuthenticationResult(result);
            }
        });
    }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username The user's username
     * @param password The user's password to be authenticated
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username,
            final String password, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                authenticate(username, password, handler, context);
            }
        };
        // run on background thread.
        return Utilities.performOnBackgroundThread(runnable);
    }
}
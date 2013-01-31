
package org.ohmage.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.ohmage.UserPreferencesHelper;

/**
 * Delegates showing login or dashboard depending on if the user is logged in
 * 
 * @author cketcham
 */
public class OhmageLauncher extends Activity {

    private static final int FINISHED = 0;
    private static final String TAG = "OhmageLauncher";
    private UserPreferencesHelper mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "OhmageLauncher is created!");

        mUser = new UserPreferencesHelper(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check account status
        if (mUser.isAuthenticated()) {
            startActivityForResult(new Intent(this, DashboardActivity.class), FINISHED);
        } else {
            startActivityForResult(new Intent(this, LoginActivity.class), FINISHED);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FINISHED:
                finish();
                break;
        }
    }
}

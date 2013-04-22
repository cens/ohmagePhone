
package org.ohmage.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TabHost;

import org.ohmage.MobilityHelper;
import org.ohmage.mobilizingcs.R;
import org.ohmage.UserPreferencesHelper;
import org.ohmage.fragments.MobilityControlFragment;
import org.ohmage.fragments.RecentMobilityChartFragment;
import org.ohmage.ui.BaseActivity;
import org.ohmage.ui.TabsAdapter;

public class MobilityActivity extends BaseActivity {

    private static final String TAG = "MobilityActivity";

    TabHost mTabHost;
    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;

    private UserPreferencesHelper mUserPrefs;

    private boolean mobilityConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserPrefs = new UserPreferencesHelper(this);

        if (!MobilityHelper.isMobilityInstalled(this)) {
            setContentView(R.layout.mobility_missing);

            // hook up market link
            Button installMobilityButton = (Button) findViewById(R.id.mobility_install_btn);

            installMobilityButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // launch the market and show mobility's details page
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(getString(R.string.mobility_market_link)));
                    startActivity(intent);
                }
            });
        } else {
            setupTabs(savedInstanceState);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupTabs(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTabHost != null)
            outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    private void setupTabs(Bundle savedInstanceState) {
        // If we haven't connected to mobility, but it is installed
        if (!mobilityConnected && MobilityHelper.isMobilityInstalled(this)) {
            mobilityConnected = true;

            setContentView(R.layout.tab_layout);
            setActionBarShadowVisibility(false);

            mTabHost = (TabHost) findViewById(android.R.id.tabhost);
            mTabHost.setup();

            mViewPager = (ViewPager) findViewById(R.id.pager);

            mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

            if (mUserPrefs.showMobilityFeedback()) {
                mTabsAdapter.addTab("Analytics", RecentMobilityChartFragment.class, null);
            } else {
                findViewById(android.R.id.tabs).setVisibility(View.GONE);
            }
            mTabsAdapter.addTab("Control", MobilityControlFragment.class, null);

            if (savedInstanceState != null) {
                mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
            }
        }
    }
}

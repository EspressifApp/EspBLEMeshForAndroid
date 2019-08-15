package com.espressif.espblemesh.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.espressif.espblemesh.R;
import com.espressif.espblemesh.app.BaseActivity;
import com.espressif.espblemesh.constants.Constants;

public class SettingsActivity extends BaseActivity implements SettingsConstants {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        getFragmentManager().beginTransaction()
                .replace(R.id.frame, new SettingsFragment())
                .commit();
    }

    public static long getUsedAppKeyIndex(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String keyIndexStr = sharedPreferences.getString(KEY_APP_USED, String.valueOf(Constants.APP_KEY_INDEX_DEFAULT));
        return Long.parseLong(keyIndexStr);
    }

    public static int getMessagePostCount(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String postCountStr = sharedPreferences.getString(KEY_MESSAGE_POST_COUNT, MESSAGE_POST_COUNT_DEFAULT);
        return Integer.parseInt(postCountStr);
    }

    public static long getMessageBackpressure(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String backpressureStr = sharedPreferences.getString(KEY_MESSAGE_BACKPRESSURE, MESSAGE_BACKPRESSURE_DEFAULT);
        return Long.parseLong(backpressureStr);
    }
}

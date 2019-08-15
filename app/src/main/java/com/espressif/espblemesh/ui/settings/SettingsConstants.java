package com.espressif.espblemesh.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.espressif.espblemesh.constants.Constants;

public interface SettingsConstants {
    String KEY_VERSION = "settings_version_key";

    String KEY_APP_NEW = "settings_app_new_key";
    String KEY_APP_DELETE = "settings_app_delete_key";
    String KEY_APP_USED = "settings_app_used_key";

    String KEY_MESSAGE_POST_COUNT = "settings_message_post_count_key";
    String KEY_MESSAGE_BACKPRESSURE = "settings_message_backpressure_key";

    String MESSAGE_POST_COUNT_DEFAULT = "3";
    String MESSAGE_BACKPRESSURE_DEFAULT = "300";
}

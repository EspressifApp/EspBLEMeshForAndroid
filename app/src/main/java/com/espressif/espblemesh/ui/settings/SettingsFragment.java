package com.espressif.espblemesh.ui.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LongSparseArray;
import android.widget.Button;

import com.espressif.blemesh.model.App;
import com.espressif.espblemesh.R;
import com.espressif.espblemesh.constants.Constants;
import com.espressif.blemesh.user.MeshUser;
import com.espressif.blemesh.task.AppAddTask;
import com.espressif.blemesh.task.AppDeleteTask;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Locale;

import libs.espressif.app.AppUtil;
import libs.espressif.utils.DataUtil;
import libs.espressif.utils.TextUtils;

public class SettingsFragment extends PreferenceFragment implements SettingsConstants,
        Preference.OnPreferenceChangeListener {
    private MeshUser mUser;

    private SharedPreferences mSharedPref;

    private EditTextPreference mAppNewPref;
    private MultiSelectListPreference mAppDelPref;
    private ListPreference mAppUsedPref;

    private EditTextPreference mMessagePostCountPref;
    private EditTextPreference mMessageBackpressurePref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        mUser = MeshUser.Instance;
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        initVersion();
        initApp();
        initMessage();
    }

    private void initVersion() {
        Preference preference = findPreference(KEY_VERSION);
        String versionName = AppUtil.getVersionName(getActivity());
        long versionCode = AppUtil.getVersionCode(getActivity());
        String summary = String.format(Locale.ENGLISH, "%s(%d)", versionName, versionCode);
        preference.setSummary(summary);
    }

    private void initApp() {
        mAppNewPref = (EditTextPreference) findPreference(KEY_APP_NEW);
        mAppNewPref.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                AlertDialog dialog = (AlertDialog) mAppNewPref.getDialog();
                if (dialog == null) {
                    return;
                }
                Button okBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (TextUtils.isEmpty(s)) {
                    mAppNewPref.getEditText().setError(null);
                    okBtn.setEnabled(false);
                    return;
                }

                if (s.length() != 32) {
                    mAppNewPref.getEditText().setError(getString(R.string.settings_new_app_key_length_error));
                    okBtn.setEnabled(false);
                    return;
                }

                String keyStr = s.toString().toLowerCase();
                for (CharSequence appKey : mAppUsedPref.getEntries()) {
                    if (appKey.toString().equals(keyStr)) {
                        mAppNewPref.getEditText().setError(getString(R.string.settings_new_app_key_duplicate_error));
                        okBtn.setEnabled(false);
                        return;
                    }
                }

                try {
                    new BigInteger(keyStr, 16);
                    mAppNewPref.getEditText().setError(null);
                    okBtn.setEnabled(true);
                } catch (Exception e) {
                    mAppNewPref.getEditText().setError(getString(R.string.settings_new_app_key_format_error));
                    okBtn.setEnabled(false);
                }
            }
        });
        mAppNewPref.setOnPreferenceChangeListener(this);

        mAppDelPref = (MultiSelectListPreference) findPreference(KEY_APP_DELETE);

        mAppUsedPref = (ListPreference) findPreference(KEY_APP_USED);
        updateAppEntriesPref();
    }

    private void updateAppEntriesPref() {
        LongSparseArray<App> appMap = mUser.getAppSparseArray();

        String usedAppKeyIndex = mSharedPref.getString(KEY_APP_USED, String.valueOf(Constants.APP_KEY_INDEX_DEFAULT));
        String summary = "";
        CharSequence[] appkeyArray = new CharSequence[appMap.size()];
        CharSequence[] appIndexArray = new CharSequence[appMap.size()];

        for (int i = 0; i < appMap.size(); i++) {
            String keyIndex = String.valueOf(appMap.keyAt(i));
            String key = DataUtil.bigEndianBytesToHexString(appMap.valueAt(i).getAppKey());
            appkeyArray[i] = key;
            appIndexArray[i] = keyIndex;
            if (usedAppKeyIndex.equals(keyIndex)) {
                summary = key;
            }

        }
        mAppUsedPref.setOnPreferenceChangeListener(null);
        mAppUsedPref.setEntries(appkeyArray);
        mAppUsedPref.setEntryValues(appIndexArray);
        mAppUsedPref.setValue(usedAppKeyIndex);
        mAppUsedPref.setSummary(summary);
        mAppUsedPref.setOnPreferenceChangeListener(this);

        appMap.remove(Constants.APP_KEY_INDEX_DEFAULT);
        appkeyArray = new CharSequence[appMap.size()];
        appIndexArray = new CharSequence[appMap.size()];
        for (int i = 0; i < appMap.size(); i++) {
            String keyIndex = String.valueOf(appMap.keyAt(i));
            String key = DataUtil.bigEndianBytesToHexString(appMap.valueAt(i).getAppKey());
            appkeyArray[i] = key;
            appIndexArray[i] = keyIndex;
        }
        mAppDelPref.setOnPreferenceChangeListener(null);
        mAppDelPref.setEntries(appkeyArray);
        mAppDelPref.setEntryValues(appIndexArray);
        mAppDelPref.setOnPreferenceChangeListener(this);

    }

    private void initMessage() {
        mMessagePostCountPref = (EditTextPreference) findPreference(KEY_MESSAGE_POST_COUNT);
        int messagePostCount = SettingsActivity.getMessagePostCount(getActivity());
        mMessagePostCountPref.setSummary(String.valueOf(messagePostCount));
        mMessagePostCountPref.setOnPreferenceChangeListener(this);

        mMessageBackpressurePref = (EditTextPreference) findPreference(KEY_MESSAGE_BACKPRESSURE);
        long messageBackpressure = SettingsActivity.getMessageBackpressure(getActivity());
        mMessageBackpressurePref.setSummary(String.valueOf(messageBackpressure));
        mMessageBackpressurePref.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAppUsedPref) {
            CharSequence[] entries = mAppUsedPref.getEntries();
            CharSequence[] values = mAppUsedPref.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                CharSequence keyIndex = values[i];
                if (keyIndex.equals(newValue)) {
                    mAppUsedPref.setSummary(entries[i]);
                    mSharedPref.edit().putString(KEY_APP_USED, keyIndex.toString()).apply();
                    return true;
                }
            }

            return true;
        } else if (preference == mAppNewPref) {
            if (TextUtils.isEmpty(newValue.toString())) {
                return true;
            }

            String appKey = newValue.toString();
            App app = new AppAddTask(appKey).run();
            if (app != null) {
                updateAppEntriesPref();
            }
        } else if (preference == mAppDelPref) {
            Collection set = (Collection) newValue;
            if (!set.isEmpty()) {
                long selectedAppIndex = SettingsActivity.getUsedAppKeyIndex(getActivity());
                for (Object obj : set) {
                    long appKeyIndex = Long.parseLong(obj.toString());
                    new AppDeleteTask(appKeyIndex).run();

                    if (selectedAppIndex == appKeyIndex) {
                        mSharedPref.edit().putString(KEY_APP_USED, String.valueOf(Constants.APP_KEY_INDEX_DEFAULT))
                                .apply();
                    }
                }

                updateAppEntriesPref();
            }

            return true;
        } else if (preference == mMessagePostCountPref) {
            String postCount = MESSAGE_POST_COUNT_DEFAULT;
            if (!TextUtils.isEmpty(newValue.toString())) {
                postCount = newValue.toString();
            }
            mMessagePostCountPref.setSummary(postCount);
            mSharedPref.edit().putString(KEY_MESSAGE_POST_COUNT, postCount).apply();
        } else if (preference == mMessageBackpressurePref) {
            String backpressure = MESSAGE_BACKPRESSURE_DEFAULT;
            if (!TextUtils.isEmpty(newValue.toString())) {
                backpressure = newValue.toString();
            }
            mMessageBackpressurePref.setSummary(backpressure);
            mSharedPref.edit().putString(KEY_MESSAGE_BACKPRESSURE, backpressure).apply();
        }
        return false;
    }
}

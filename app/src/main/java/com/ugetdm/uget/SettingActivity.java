package com.ugetdm.uget;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;
//import android.util.Log;

public class SettingActivity extends PreferenceActivity {
	// Application data
	public static MainApp app = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (MainApp)getApplicationContext();
		ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(R.string.menu_settings);

        if(hasHeaders()) {
        }
    }

    @Override
    protected void onPause() {
    	app.getSettingFromPreferences();
    	app.applySetting();
    	super.onPause();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
        loadHeadersFromResource(R.xml.header, target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return UiSettingFragment.class.getName().equals(fragmentName)
                || ClipboardSettingFragment.class.getName().equals(fragmentName)
                || SpeedSettingFragment.class.getName().equals(fragmentName)
                || Aria2SettingFragment.class.getName().equals(fragmentName)
                || MediaSettingFragment.class.getName().equals(fragmentName)
                || OtherSettingFragment.class.getName().equals(fragmentName);
    }

    // header Fragment
    //
    public static class UiSettingFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_ui);
        }
    }

    public static class ClipboardSettingFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_clipboard);
        }
    }

    public static class SpeedSettingFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_speed);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePreferenceSummary();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePreferenceSummary();
        }

        private void updatePreferenceSummary() {
            Preference preference;
            SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();

            preference = findPreference("preference_speed_download");
            preference.setSummary(sharedPrefs.getString("preference_speed_download", "0") + " KiB/s");

            preference = findPreference("preference_speed_upload");
            preference.setSummary(sharedPrefs.getString("preference_speed_upload", "0") + " KiB/s");

            // if (preference instanceof ListPreference) {
            //     ListPreference listPreference = (ListPreference) preference;
            //     listPreference.setSummary(listPreference.getEntry());
            //     return;
            // }
        }
    }

    public static class Aria2SettingFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_aria2);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            Preference.OnPreferenceClickListener clickListener;
            clickListener = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // TODO Auto-generated method stub
                    app.preferenceAria2Changed = true;
                    return false;
                }
            };

            Preference preference;
            // preference_aria2_uri
            preference = findPreference("preference_aria2_uri");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_token");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_speed_download");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_speed_upload");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_local");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_launch");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_shutdown");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_path");
            preference.setOnPreferenceClickListener(clickListener);
            preference = findPreference("preference_aria2_args");
            preference.setOnPreferenceClickListener(clickListener);
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePreferenceSummary();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePreferenceSummary();
        }

        private void updatePreferenceSummary() {
            Preference preference;
            SharedPreferences sharedPrefs = getPreferenceManager().getSharedPreferences();

            preference = findPreference("preference_aria2_uri");
            preference.setSummary(sharedPrefs.getString("preference_aria2_uri", "http://localhost:6800/jsonrpc"));

            preference = findPreference("preference_aria2_speed_download");
            preference.setSummary(sharedPrefs.getString("preference_aria2_speed_download", "0") + " KiB/s");
            preference = findPreference("preference_aria2_speed_upload");
            preference.setSummary(sharedPrefs.getString("preference_aria2_speed_upload", "0") + " KiB/s");

            // if (preference instanceof ListPreference) {
            //     ListPreference listPreference = (ListPreference) preference;
            //     listPreference.setSummary(listPreference.getEntry());
            //     return;
            // }
        }
    }

    public static class MediaSettingFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_media);
        }
    }

    public static class OtherSettingFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_other);
        }
    }
}

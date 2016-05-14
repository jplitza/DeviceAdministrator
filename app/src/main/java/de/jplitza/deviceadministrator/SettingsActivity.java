package de.jplitza.deviceadministrator;


import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("deprecation")
public class SettingsActivity extends AppCompatPreferenceActivity {
    private Activity mSettingsActivity;

    static private ArrayMap<String, String[]> perms = new ArrayMap<>(3);

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName);
    }

    private class BindPreferenceRequestPermissions implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            if (!(boolean)o)
                return !preference.getKey().equals("basic_permissions_granted");

            List<String> missingPerms = new LinkedList<>();
            for (String perm : perms.get(preference.getKey())){
                if ((perm.equals(Manifest.permission.BIND_DEVICE_ADMIN) && !DeviceAdmin.getDPM(mSettingsActivity).isAdminActive(DeviceAdmin.getComponentName(mSettingsActivity)))
                || (!perm.equals(Manifest.permission.BIND_DEVICE_ADMIN) && ContextCompat.checkSelfPermission(mSettingsActivity, perm) != PackageManager.PERMISSION_GRANTED))
                    missingPerms.add(perm);
            }
            if (missingPerms.isEmpty())
                return true;

            ActivityCompat.requestPermissions(
                    mSettingsActivity,
                    missingPerms.toArray(new String[missingPerms.size()]),
                    perms.indexOfKey(preference.getKey())
            );
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        boolean granted = true;
        boolean need_device_admin = false;
        for (String perm : perms.valueAt(requestCode)) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(perm)) {
                    if (perm.equals(Manifest.permission.BIND_DEVICE_ADMIN)) {
                        need_device_admin = true;
                    } else {
                        granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }
                    break;
                }
            }
            if (!granted)
                break;
        }
        if (granted && need_device_admin) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DeviceAdmin.getComponentName(this));
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, findPreference(perms.keyAt(requestCode)).getSummary());
            startActivityForResult(intent, requestCode);
        }
        else {
            Log.d("RequestPermissionsResul", "Setting checkbox " + perms.keyAt(requestCode) + " to " + granted);
            ((CheckBoxPreference)findPreference(perms.keyAt(requestCode))).setChecked(granted);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ((CheckBoxPreference)findPreference(perms.keyAt(requestCode))).setChecked(resultCode == Activity.RESULT_OK);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettingsActivity = this;
        addPreferencesFromResource(R.xml.pref_general);
        setupActionBar();

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences
        // to their values. When their values change, their summaries are
        // updated to reflect the new value, per the Android Design
        // guidelines.
        bindPreferenceSummaryToValue(findPreference("seqnum"));

        perms.put("basic_permissions_granted", new String[]{
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
        });
        perms.put("allow_location", new String[]{
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
        });
        perms.put("allow_location_modechange", new String[]{
                Manifest.permission.BIND_DEVICE_ADMIN,
        });
        perms.put("allow_wipe", new String[]{
                Manifest.permission.BIND_DEVICE_ADMIN,
        });
        for (String key : perms.keySet())
            findPreference(key)
                    .setOnPreferenceChangeListener(new BindPreferenceRequestPermissions());
    }
}

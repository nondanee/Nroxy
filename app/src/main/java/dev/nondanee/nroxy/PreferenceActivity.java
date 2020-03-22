package dev.nondanee.nroxy;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.Preference.OnPreferenceChangeListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Set;

public class PreferenceActivity extends AppCompatActivity {
    private static final String TAG = Constant.DEBUG_TAG + ".Preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);
        Toolbar toolbar = findViewById(R.id.toolbar_preference);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences sharedPreferences;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Preference version = findPreference("version");
            version.setSummary(BuildConfig.VERSION_NAME + " " + "(" + BuildConfig.VERSION_CODE + ")");

            CheckBoxPreference script = (CheckBoxPreference) findPreference("script");
            script.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value)
                {
                    boolean checked = Boolean.valueOf(value.toString());
                    if (checked) {
                        selectFile();
                        return false;
                    }
                    else {
                        sharedPreferences.edit().putString("path", null).apply();
                        return true;
                    }
                }
            });
        }

        private boolean permitStorage() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (getActivity().checkSelfPermission(Constant.PERMISSIONS_STORAGE[0]) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(Constant.PERMISSIONS_STORAGE, Constant.CODE_REQUEST_STORAGE);
                        return false;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        private void selectFile() {
            if (!permitStorage()) return;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_script_file)), Constant.CODE_CHOOSE_FILE);
            }
            catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), getString(R.string.install_file_manager), Toast.LENGTH_SHORT).show();
            }
        }

        private void accessFile(Uri uri) {
            String path = Utility.getFilePath(getActivity(), uri);
            sharedPreferences.edit().putString("path", path).apply();
            if (path != null) sharedPreferences.edit().putBoolean("script", true).apply();
            if (path == null) Toast.makeText(getActivity(), getString(R.string.access_file_failed), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == Constant.CODE_REQUEST_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED) selectFile();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == Constant.CODE_CHOOSE_FILE && resultCode == Activity.RESULT_OK) accessFile(data.getData());
        }

        @Override
        public void onResume() {
            super.onResume();

            sharedPreferences = getPreferenceManager().getSharedPreferences();
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            refreshSummary(getPreferenceScreen());
        }

        @Override
        public void onPause() {
            super.onPause();

            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = getPreferenceScreen().findPreference(key);
            if (preference != null) setSummary(preference);
        }

        private void setApplications(ApplicationListPreference preference) {
            Set<String> applications = preference.getValues();
            int amount = applications.size();
            if (amount == 0)
                preference.setSummary(getString(R.string.global));
            else
                preference.setSummary(amount + " " + getString(amount == 1 ? R.string.application : R.string.applications));
        }

        private void setScript(CheckBoxPreference preference) {
            String path = sharedPreferences.getString("path", null);
            preference.setSummary(path);
            preference.setChecked(path != null);
        }

        private void setType(ListPreference preference) {
            String value = preference.getValue();
            preference.setSummary(getString("allow".equals(value) ? R.string.whitelist : R.string.blacklist));
        }

        private void setSummary(Preference preference) {
            String key = preference.getKey();
            if (key == null) return;
            if (key.equals("applications")) {
                setApplications((ApplicationListPreference) preference);
            }
            else if (key.equals("script")) {
                setScript((CheckBoxPreference) preference);
            }
            else if (key.equals("type")) {
                setType((ListPreference) preference);
            }
            else if (preference instanceof EditTextPreference) {
                String text = ((EditTextPreference) preference).getText();
                preference.setSummary(text == null || "".equals(text) ? getString(R.string.none) : text);
            }
            else if (preference instanceof ListPreference) {
                preference.setSummary(((ListPreference) preference).getValue());
            }
            else if (preference instanceof MultiSelectListPreference) {
                preference.setSummary(Arrays.toString(((MultiSelectListPreference) preference).getValues().toArray()));
            }
        }

        private void refreshSummary(Preference preference) {
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                    refreshSummary(preferenceGroup.getPreference(i));
                }
            }
            else {
                setSummary(preference);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.preference, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.action_help) startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constant.HELP_LINK)));
        return false;
    }
}
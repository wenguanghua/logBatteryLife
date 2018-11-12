package w.tpbatterylife;

import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class SettingsActivity extends AppCompatActivity {
    static public final String SCAN_ENABLED = "switch_scan";
    static public final String SCAN_INTERVAL = "edit_scanInterval";
    static public final String LOG_ENABLED = "switch_log";
    static public final String LOG_TIMING = "list_logTiming";
    static public final String LOGGING_INTERVAL = "edit_loggingInterval";
    static public final String AUTO_RUN = "switch_autorun";

    static boolean logEnable = false;
    static boolean intarvalEnable = false;
    static boolean scanEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // PrefFragmentの呼び出し
        getFragmentManager().beginTransaction().replace(
                android.R.id.content, new PrefFragment()).commit();
    }

    // 設定画面のPrefFragmentクラス
    public static class PrefFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            updatePreference();
        }

        // 設定値が変更されたときのリスナーを登録
        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.registerOnSharedPreferenceChangeListener(listener);
        }

        // 設定値が変更されたときのリスナー登録を解除
        @Override
        public void onPause() {
            super.onPause();
            SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            sp.unregisterOnSharedPreferenceChangeListener(listener);
        }

        // 設定変更時に、Summaryを更新
        private SharedPreferences.OnSharedPreferenceChangeListener listener
                = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(
                    SharedPreferences sharedPreferences, String key) {
                Log.d("tp settings", "listener:"+key);
//                if (key.equals(SCAN_ENABLED)) {
//                    setScanEnabledSwitch();
//                } else if (key.equals(SCAN_INTERVAL)) {
//                    setScanIntervalSummary();
//                } else if (key.equals(LOGGING_INTERVAL)) {
//                    setLoggingIntervalSummary();
//                } else if (key.equals(LOG_ENABLED)) {
//                    setLogEnabledSwitch();
//                } else if (key.equals(LOG_TIMING)) {
//                    setLogTimingSelect();
//                } else if (key.equals(AUTO_RUN)) {
//                    setAutoRunSwitch();
//                }
                switch (key) {
                    case SCAN_ENABLED:
                        setScanEnabledSwitch();
                        break;
                    case SCAN_INTERVAL:
                        setScanIntervalSummary();
                        break;
                    case LOGGING_INTERVAL:
                        setLoggingIntervalSummary();
                        break;
                    case LOG_ENABLED:
                        setLogEnabledSwitch();
                        break;
                    case LOG_TIMING:
                        setLogTimingSelect();
                        break;
                    case AUTO_RUN:
                        setAutoRunSwitch();
                        break;
                    default:
                        break;
                }
            }
        };

        private void updatePreference() {
            setScanEnabledSwitch();
            setScanIntervalSummary();
            setLogEnabledSwitch();
            setLogTimingSelect();
            setLoggingIntervalSummary();
            setAutoRunSwitch();
        }

        private void setScanEnabledSwitch() {
            SwitchPreference pref = (SwitchPreference) findPreference(SCAN_ENABLED);
            pref.setChecked(pref.isChecked());
            scanEnabled = pref.isChecked();

            EditTextPreference prefInterval = (EditTextPreference) findPreference(SCAN_INTERVAL);
            prefInterval.setEnabled(scanEnabled);
        }
        private void setScanIntervalSummary() {
            EditTextPreference pref = (EditTextPreference) findPreference(SCAN_INTERVAL);
            pref.setSummary(pref.getText());
        }
        private void setAutoRunSwitch() {
            SwitchPreference pref = (SwitchPreference) findPreference(AUTO_RUN);
            pref.setChecked(pref.isChecked());
        }

        private void setLogEnabledSwitch() {
            SwitchPreference pref = (SwitchPreference) findPreference(LOG_ENABLED);
            logEnable = pref.isChecked();
            pref.setChecked(logEnable);

            ListPreference prefList = (ListPreference) findPreference(LOG_TIMING);
            prefList.setEnabled(logEnable);

            setLoggingIntervalSummary();
        }

        private void setLogTimingSelect() {
            ListPreference pref = (ListPreference) findPreference(LOG_TIMING);
            pref.setSummary(pref.getEntry() == null ? "" : pref.getEntry());
            Log.d("tp settings", "setLogTimingSelect");
            int t = Integer.parseInt(pref.getValue());
            intarvalEnable = (t == MainActivity.TIMING_POLLING);
            setLoggingIntervalSummary();
        }

        private void setLoggingIntervalSummary() {
            EditTextPreference pref = (EditTextPreference) findPreference(LOGGING_INTERVAL);
            pref.setSummary(pref.getText());
            pref.setEnabled(intarvalEnable && logEnable);
        }

//        private void setLogChangeOnlyBox() {
//            CheckBoxPreference pref = (CheckBoxPreference) findPreference(LOG_STATE_CHANGE_ONLY);
//            pref.setChecked(pref.isChecked());
//            pref.setEnabled(logSwitch);
//
//            EditTextPreference editLogInterval = (EditTextPreference) findPreference(LOGGING_INTERVAL);
//            editLogInterval.setEnabled(!pref.isChecked() && logSwitch);
//        }
    }
}

package w.tpbatterylife;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.extbcr.scannersdk.BarcodeData;
import com.extbcr.scannersdk.BarcodeManager;
import com.extbcr.scannersdk.EventListener;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "tpBatteryLife";

    TextView textViewStatus;
    TextView textViewHealth;
    TextView textViewLevel;
    TextView textViewVoltage;
    TextView textViewTemperature;
    TextView textViewChargeCycle;
    TextView textViewChargeFull;
    TextView textViewChargeFullDesign;
    TextView textViewChargeNow;
    TextView textViewLog;
    TextView textViewLogging;

    TextView textViewTitle;
    TextView textViewBarcode;
    TextView textViewElapsed;

    Button buttonTest;

    int status;
    int health;
    boolean present;
    int level;
    int scale;
    int icon_small;
    int plugged;
    int voltage;
    int temperature;
    String technology;

    long currentNow;
    long chargeFull;
    long chargeNow;
    long chargeFullDesign;
    long chargeCycle;

    String statusString = "";
    String healthString = "";
    String acString = "";
    boolean broadcast;
    boolean logEnabled = false;
    boolean logStateChangeOnly = false;
    int loggingInterval;
    int changedCount = 0;
    File logFile = null;
    boolean logTitle = false;

    // バーコード
    private BarcodeManager mBarcodeManager;
    private EventListener mEventListener;
    private boolean scanServerConnect = false;
    private int scanInterval;
    private boolean scanEnabled;

    public static final int TIMING_SCAN_EVENT = 1;
    public static final int TIMING_POLLING = 2;
    public static final int TIMING_BROADCAST = 3;
    int logTiming;
    String lastBarcode = "";
    final static float MIN_BRIGHTNESS = 0.0f;
    float preScreenBrightness; // [0.0-1.0]
    boolean preAirplaneMode;
    boolean preBluetoothState;
    boolean preWifiState;

    int tick;
    private long startTime;
    String elapsedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate");
        setupView();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryChangeReceiver, intentFilter);
        broadcast = false;
    }

    private void setupView() {
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewHealth = findViewById(R.id.textViewHealth);
        textViewLevel = findViewById(R.id.textViewLevel);
        textViewVoltage = findViewById(R.id.textViewVoltage);
        textViewTemperature = findViewById(R.id.textViewTemperature);

        textViewChargeCycle = findViewById(R.id.textViewCycle);
        textViewChargeFull = findViewById(R.id.textViewChargeFull);
        textViewChargeFullDesign = findViewById(R.id.textViewFullDesign);
        textViewChargeNow = findViewById(R.id.textViewChargeNow);

        textViewTitle = findViewById(R.id.textViewTitle);
        textViewBarcode = findViewById(R.id.textViewBarcode);
        textViewElapsed = findViewById(R.id.textViewElapsed);

        buttonTest = findViewById(R.id.button);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String buttonText = buttonTest.getText().toString();
                Log.d(TAG, "Button: " + buttonText + ", R:" + getString(R.string.button_start_test));
                if (buttonText.equals(getString(R.string.button_start_test))) {
                    startTest();
                } else {
                    stopTest();
                }
            }
        });

        textViewLog = findViewById(R.id.textViewLog);
        textViewLogging = findViewById(R.id.textViewLogging);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate our menu from the resources by using the menu inflater.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                // Here we would open up our settings activity
                Log.d(TAG, "Menu Setting is selected");
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e(TAG, "onDestroy+");
        if (broadcast)
            unregisterReceiver(batteryChangeReceiver);

        broadcast = false;
        Log.d(TAG, "onDestroy-");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        getSettings();
        updateViewText();

        if (!broadcast) {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(batteryChangeReceiver, ifilter);
            broadcast = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop+");

        if (!logEnabled) {
            unregisterReceiver(batteryChangeReceiver);
            broadcast = false;
        }
        Log.d(TAG, "onStop-");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        updateFuelGaugeInfo();
        initScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        deinitScan();
        stopTest();
        intervalHandler.removeCallbacks(runnable);
    }

    private void startTest() {
        buttonTest.setText(R.string.buttno_stop_test);
        textViewTitle.setKeepScreenOn(true);

        preScreenBrightness = getScreenBrightness();

        setScreenBrightness(MIN_BRIGHTNESS);
        preAirplaneMode = getAirplaneMode(getApplicationContext());
        if (!preAirplaneMode) {
            setAirplaneMode(getApplicationContext(), true);
        }
        preBluetoothState = getBluetoothState();
        if (preBluetoothState) {
            setBluetoothState(false);
        }
        preWifiState = getWifiState();
        if (preWifiState) {
            setWifiState(false);
        }

//        updateFuelGaugeInfo();
        logTestStart();
        tick = 0;
        startTime = System.currentTimeMillis();
        intervalHandler.post(runnable);
        Log.e(TAG, "start battery test...");
    }

    private void stopTest() {
        setScreenBrightness(preScreenBrightness);
        setAirplaneMode(getApplicationContext(), preAirplaneMode);
        setBluetoothState(preBluetoothState);
        setWifiState(preWifiState);

        buttonTest.setText(R.string.button_start_test);
        intervalHandler.removeCallbacks(runnable);
        updateTextViewBarcode();
        logTestEnd();
        Log.d(TAG, "stopTest");
    }

    private void updateViewText() {
        textViewTitle.setText(R.string.test_title);

        updateTextViewBarcode();

        String logStatus;
        if (logEnabled) {
            if (logTiming == TIMING_BROADCAST) {
                logStatus = getString(R.string.log_broadcast);
            } else {
                logStatus = getString(R.string.log_polling) + Integer.toString(loggingInterval) + " s";
            }
        } else {
            logStatus = getString(R.string.log_disabled);
        }
        textViewLog.setText(logStatus);
        textViewLogging.setText("");
    }

    private void updateTextViewBarcode() {
        if (!scanEnabled) {
            textViewBarcode.setText(R.string.scan_disabled);
        } else {
            String temp = "scan interval is " + Integer.toString(scanInterval) + " s";
            textViewBarcode.setText(temp);
        }
    }

    private BroadcastReceiver batteryChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                Log.d(TAG, "BroadcastReceiver...");

                status = intent.getIntExtra("status", 0);
                health = intent.getIntExtra("health", 0);
                present = intent.getBooleanExtra("present", false);
                level = intent.getIntExtra("level", 0);
                scale = intent.getIntExtra("scale", 0);
                icon_small = intent.getIntExtra("icon-small", 0);
                plugged = intent.getIntExtra("plugged", 0);
                voltage = intent.getIntExtra("voltage", 0);
                temperature = intent.getIntExtra("temperature", 0);
                technology = intent.getStringExtra("technology");

                updateButteryInfoTestView();

                if (logEnabled && (logTiming == TIMING_BROADCAST)) {
                    textViewLogging.setText("!");
                    changedCount++;
                    logBatteryStatus();
                }
            }
        }
    };

    private void updateButteryInfoTestView() {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                statusString = "unknown";
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusString = "charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusString = "discharging";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusString = "not charging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusString = "full";
                break;
        }

        switch (health) {
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                healthString = "unknown";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthString = "good";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthString = "overheat";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthString = "dead";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthString = "voltage";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                healthString = "unspecified failure";
                break;
        }

        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                acString = "plugged ac";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                acString = "plugged usb";
                break;
        }

        textViewStatus.setText(statusString);
        textViewHealth.setText(healthString);
        String tempString = String.valueOf(level) + "%";
        textViewLevel.setText(tempString);
        tempString = String.valueOf(voltage / 1000) + "." + String.valueOf(voltage % 1000) + " V";
        textViewVoltage.setText(tempString);
        tempString = String.valueOf(temperature / 10) + "." + String.valueOf(temperature % 10) + " ℃";
        textViewTemperature.setText(tempString);

        Log.v("status", statusString);
        Log.v("health", healthString);
        Log.v("present", String.valueOf(present));
        Log.v("level", String.valueOf(level));
        Log.v("scale", String.valueOf(scale));
        Log.v("icon_small", String.valueOf(icon_small));
        Log.v("plugged", acString);
        Log.v("voltage", String.valueOf(voltage));
        Log.v("temperature", String.valueOf(temperature));
        Log.v("technology", technology);
    }

    //周期動作処理
    final Handler intervalHandler = new Handler();
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Log.d("tpBattery", "interval run...");
            intervalHandler.postDelayed(this, 1000);
            elapsedTime = getElapsedTime();
            textViewElapsed.setText(elapsedTime);

            //読取
            if (scanEnabled) {
                int remainedCount = scanInterval - (tick % scanInterval);
                String remainedCountString = "scan after..." + Integer.toString(remainedCount) + "s";
                textViewBarcode.setText(remainedCountString);
                Log.d(TAG, remainedCountString);

                if (tick % scanInterval == 0) {
                    Log.d(TAG, "start scan");
                    textViewBarcode.setText("");
                    startScan();
                }
            }

            //updateFuelGaugeInfo();

            // ログ
            if (logEnabled) {
                if ((logTiming == TIMING_POLLING) && (tick % loggingInterval == 0)) {
                    textViewLogging.setText("!");
                    logBatteryStatus();
                } else
                    textViewLogging.setText((tick % 2 == 0) ? "..." : "");
            }
            tick++;
        }
    };

    //ログ部
    private void logTestStart() {
        Log.d("tpBatteryLife", "logTestStart");
        if (logFile == null) {
            logFile = new File(getExternalFilesDir(null), "logBatteryLife.txt");
            Log.d(TAG, "created log file");
        }

        String batteryCondition = "\n"
                + "Test start: " + getNowDate() + "\n"
                + "currentNow = " + Long.toString(currentNow) + "\n"
                + "chargeCycle = " + Long.toString(chargeCycle) + "\n"
                + "chargeFull = " + Long.toString(chargeFull) + "\n"
                + "chargeFullDesign = " + Long.toString(chargeFullDesign) + "\n"
                + "chargeNow = " + Long.toString(chargeNow) + "\n"
                + "screenBrightness = " + Float.toString(MIN_BRIGHTNESS) + "\n"
                + "barcode = " + scanEnabled
                + "\n";

        OneLineIo.logString(logFile, batteryCondition);
    }

    private void logTestEnd() {
        if (logFile == null) {
            return;
        }
        String end  = "Test end: " + getNowDate() + ", elapsed: " + elapsedTime +"\n";
        OneLineIo.logString(logFile, end);
    }

    private void logBatteryStatus() {
        if (logFile == null) {
            return;
        }

        if (!logTitle) {
            String title = "count,elapsed,status,level,ac,voltage,temperature\n";
            OneLineIo.logString(logFile, title);
            logTitle = true;
        }
        int count;
        if (logTiming == TIMING_BROADCAST) {
            count = changedCount;
        } else {
            count = tick / loggingInterval;
        }

        //String elapsed = (logTiming == TIMING_SCAN_EVENT) ? elapsedTime : getElapsedTime();
        String batteryLogString =
                Integer.toString(count) + "," +
                        elapsedTime + "," +
                        statusString + "," +
                        Integer.toString((level)) + "," +
                        acString + "," +
                        Integer.toString(voltage) + "," +
                        Integer.toString(temperature) +
                        lastBarcode + "\n";

        OneLineIo.logString(logFile, batteryLogString);
        Log.d(TAG, "logBatteryStatus-");
    }

    public static String getNowDate() {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN);
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    private void getFuelGaugeInfo() {
        File f = new File("/sys/class/power_supply/bq27500/current_now");
        if (f.exists()) {
            currentNow = OneLineIo.getValue(f) / 1000; // micro mA-> mA
        }
        Log.d(TAG, "bq27500/current_now =  " + currentNow + "mA");

        f = new File("/sys/class/power_supply/bq27500/cycle_count");
        if (f.exists()) {
            chargeCycle = OneLineIo.getValue(f);
        }

        f = new File("/sys/class/power_supply/bq27500/charge_full");
        if (f.exists()) {
            chargeFull = OneLineIo.getValue(f); // micro mAh
        }

        f = new File("/sys/class/power_supply/bq27500/charge_full_design");
        if (f.exists()) {
            chargeFullDesign = OneLineIo.getValue(f); // micro mAh
        }

        f = new File("/sys/class/power_supply/bq27500/charge_now");
        if (f.exists()) {
            chargeNow = OneLineIo.getValue(f); // micro mAh
        }
    }

    private void updateFuelGaugeInfo() {
        getFuelGaugeInfo();

        String temp = Long.toString(chargeCycle);
        textViewChargeCycle.setText(temp);
        temp = Long.toString(chargeFull / 1000) + " mAh";
        textViewChargeFull.setText(temp);
        temp = Long.toString(chargeFullDesign / 1000) + " mAh";
        textViewChargeFullDesign.setText(temp);
        temp = Long.toString(chargeNow / 1000) + " mAh";
        textViewChargeNow.setText(temp);
    }

    //設定部
    private void getSettings() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        scanEnabled = pref.getBoolean(SettingsActivity.SCAN_ENABLED, true);

        String tempString = pref.getString(SettingsActivity.SCAN_INTERVAL, "5");
        scanInterval = Integer.parseInt(tempString);

        logEnabled = pref.getBoolean(SettingsActivity.LOG_ENABLED, false);

        tempString = pref.getString(SettingsActivity.LOG_TIMING, "1");
        logTiming = Integer.parseInt(tempString);

        tempString = pref.getString(SettingsActivity.LOGGING_INTERVAL, "60");
        loggingInterval = Integer.parseInt(tempString);
        if (logTiming == TIMING_SCAN_EVENT) {
            loggingInterval = scanInterval;
        }

        Log.d(TAG, "scanInterval: " + scanInterval);
        Log.d(TAG, "logEnabled: " + logEnabled);
        Log.d(TAG, "logFuelGauge: " + logStateChangeOnly);
        Log.d(TAG, "loggingInterval: " + loggingInterval);
    }

    //読取部
    private void initScan() {
        Log.e(TAG, "initScan: XXX");
        mBarcodeManager = new BarcodeManager(this);
        mBarcodeManager.init();

        mEventListener = new EventListener() {

            @Override
            public void onReadData(BarcodeData result) {
                Log.e(TAG, "onReadData " + result.getText());
                doScanResult(result.getText());
            }

            @Override
            public void onTimeout() {
                Log.e(TAG, "onTimeout");
                doScanResult("timeout");
            }

            @Override
            public void onConnect() {
                Log.e(TAG, "onConnect");
                scanServerConnect = true;

            }

            @Override
            public void onDisconnect() {
                Log.e(TAG, "onDisconnect");
                scanServerConnect = false;
            }

            @Override
            public void onStart() {
                Log.e(TAG, "onStart");
            }

            @Override
            public void onStop() {
                Log.e(TAG, "onStop");
            }
        };

        mBarcodeManager.addListener(mEventListener);
    }

    private void deinitScan() {
        Log.d(TAG, "deinitScan");
        try {
            if (scanServerConnect) {
                mBarcodeManager.stopDecode();
            }
            mBarcodeManager.removeListener();
            mBarcodeManager.deinit();
        } catch (Exception e) {
            Log.d(TAG, "onDestroy: mBarcodeManager exception!");
            e.printStackTrace();
        }
    }

    private void startScan() {
        Log.e(TAG, "startScan");

        try {
            if (scanServerConnect) {
                mBarcodeManager.startDecode();
            }
        } catch (Exception e) {
            Log.e(TAG, "exception on ScanBarcode");
        }
    }

    private void stopScan() {
        Log.e(TAG, "stopScan");

        try {
            if (scanServerConnect) {
                mBarcodeManager.stopDecode();
            }
        } catch (Exception e) {
            Log.e(TAG, "exception on ScanBarcode");
        }
    }

    private void doScanResult(String stringResult) {
        Log.d(TAG, "doScanResult+ " + stringResult);

        stopScan();
        textViewBarcode.setText(stringResult);
        lastBarcode = stringResult;

        if (logEnabled && (logTiming == TIMING_SCAN_EVENT)) {
            textViewLogging.setText("!");
            logBatteryStatus();
        }

        Log.d(TAG, "doScanResult-");
    }
    private float getScreenBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        return lp.screenBrightness;
    }

    private void setScreenBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }

    private String getElapsedTime() {
        long endTime = System.currentTimeMillis();
        long diffTime = (endTime - startTime);
        long hh = diffTime / 1000 / (60 * 60); // 時
        long mm = (diffTime / 1000 / 60) % 60; // 分
        long ss = diffTime / 1000 % 60; // 秒
        return String.format(Locale.JAPAN,"%d:%02d:%02d", hh, mm, ss);
    }

    private boolean getAirplaneMode(Context context) {
        try {
            int airplaneModeSetting = Settings.System.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
            Log.d(TAG, "airplane mode: " + airplaneModeSetting);
            return (airplaneModeSetting == 1);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "airplane mode: ..." + e.toString());
            return false;
        }
    }

    private void setAirplaneMode(Context context, boolean enabled) {
        Log.d(TAG, "set airplane mode+ " + enabled);
        try {
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enabled? 1 : 0);
        } finally {
            Log.d(TAG, "set airplane mode- " + enabled);
        }
    }

    private boolean getBluetoothState() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter.isEnabled();
    }
    private void setBluetoothState(boolean enabled) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (enabled) {
            mBluetoothAdapter.enable();
        } else {
            mBluetoothAdapter.disable();
        }
    }

    private boolean getWifiState() {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wifi.isWifiEnabled();
        } catch (Exception e) {
            Log.d(TAG, "getWifiState: " + e.toString());
            return  false;
        }
    }

    private void setWifiState(boolean enabled) {
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(enabled);
        } catch (Exception e) {
            Log.d(TAG, "setWifiState: " + e.toString());
        }
    }
}


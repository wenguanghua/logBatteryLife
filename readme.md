memo_tpBatteryLife
==================


機能
-------
◇バッテリ情報を取得
・パワーステタースとバッテリ情報の取得と表示
  status,level,plugged,voltage,temperature   ==> broadcast
  currentNow,chargeCycle,chargeFull,chargeFullDesign,chargeNow   ==>Polling

◇読取
・一定間隔で読取する（初期値は５秒）

◇記録
　上記情報をlogする：
  storage>self>primary>Android>data>w.tpbatteryLife>files>logBatteryLife.txt

  count,date,status,level,plugged,voltage,temperature,
　・開始するときのlog：
　　・開始時刻
　　・開始時のバッテリー条件・状態
	　currentNow,chargeCycle,chargeFull,chargeFullDesign,chargeNow
　　・開始時のスクリーンの明るさ
　・終了時点のlog
　　・終了時刻
　　・経過時間

◇設定
　バーコードスキャン間隔（初期値：5秒）
　LogのEnabled/Disableb
　Loggingのタイミング選択
　スキャンするとき
　・定期的
　・バッテリー状態変化があるとき
　Logの間隔（default:60秒）
　reboot後自動起動するかを選択可　＝＞残量0から記録可能に

◇環境設定
・テスト起動時スクリーン輝度を最小限に設定
・テスト起動時に飛行モードオン、BluetoothとWiFiもOFF
　テスト終了時にもとの輝度無線設定に戻す

◇画面表示
・バッテリー状態を表示
・APP起動時やテスト開始時にバッテリー情報を表示
・スキャン間隔情報表示
・ログ設定情報やログ状態を表示
・経過時間を表示

・H-28/H-29画面サイズは違うので、バッテリー情報の表示は左揃い

トラブル対応
-------------

◇resource参照できない
・エラー
　array.xmlに記述したstring使用できない＝＞ビルドエラー
　または、incompatible with attribute android:entries (attr) reference.

    <ListPreference
        android:key="list_logTiming"
        android:title="Log with scan event"
        android:entries="@array/logTimingListEntry"
        android:entryValues="@array/logTimingListValues"
        android:defaultValue="1" />

・原因：
　array.xmlは「xml」フォルダに入れた。正解は、「values」フォルダ

◇exception
・現象
　LogのタイミングはBroadcastに指定した後に、crashは発生。

11-09 14:17:40.792 13822-13822/w.tpbatterylife E/AndroidRuntime: FATAL EXCEPTION: main
                                                                 Process: w.tpbatterylife, PID: 13822
                                                                 java.lang.RuntimeException: Error receiving broadcast Intent { act=android.intent.action.BATTERY_CHANGED flg=0x60000010 (has extras) } in w.tpbatterylife.MainActivity$2@d53d4db
                                                                     at android.app.LoadedApk$ReceiverDispatcher$Args.run(LoadedApk.java:891)
                                                                     at android.os.Handler.handleCallback(Handler.java:739)
                                                                     at android.os.Handler.dispatchMessage(Handler.java:95)
                                                                     at android.os.Looper.loop(Looper.java:148)
                                                                     at android.app.ActivityThread.main(ActivityThread.java:5417)
                                                                     at java.lang.reflect.Method.invoke(Native Method)
                                                                     at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726)
                                                                     at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)
                                                                  Caused by: java.lang.NullPointerException: file == null
                                                                     at java.io.FileOutputStream.<init>(FileOutputStream.java:84)
                                                                     at w.tpbatterylife.OneLineIo.logString(OneLineIo.java:55)
                                                                     at w.tpbatterylife.MainActivity.logBatteryStatus(MainActivity.java:412)
                                                                     at w.tpbatterylife.MainActivity.access$600(MainActivity.java:32)
                                                                     at w.tpbatterylife.MainActivity$2.onReceive(MainActivity.java:276)
                                                                     at android.app.LoadedApk$ReceiverDispatcher$Args.run(LoadedApk.java:881)
                                                                     at android.os.Handler.handleCallback(Handler.java:739) 
                                                                     at android.os.Handler.dispatchMessage(Handler.java:95) 
                                                                     at android.os.Looper.loop(Looper.java:148) 
                                                                     at android.app.ActivityThread.main(ActivityThread.java:5417) 
                                                                     at java.lang.reflect.Method.invoke(Native Method) 
                                                                     at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726) 
                                                                     at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616) 
・原因
　これは、BatteryのBroadcastReceiver内でlogStringするから。
　最初の時点でテスト未開始,fileを未作成でした。しかし、Broadcastが発生する可能性がある。

・対策
　file==nullの場合logしない

◇Airplane
11-09 18:20:41.235 22665-22665/w.tpbatterylife D/tpBattery: setAirplaneMode:true
11-09 18:20:41.236 22665-22665/w.tpbatterylife W/Settings: Setting airplane_mode_on has moved from android.provider.Settings.System to android.provider.Settings.Global, value is unchanged.


11-09 18:55:01.770 22665-22665/w.tpbatterylife D/tpBattery: setAirplaneMode:false
11-09 18:55:01.775 22665-22665/w.tpbatterylife E/AndroidRuntime: FATAL EXCEPTION: main
                                                                 Process: w.tpbatterylife, PID: 22665
                                                                 java.lang.RuntimeException: Unable to pause activity {w.tpbatterylife/w.tpbatterylife.MainActivity}: java.lang.SecurityException: Permission denial: writing to settings requires:android.permission.WRITE_SECURE_SETTINGS
                                                                     at android.app.ActivityThread.performPauseActivity(ActivityThread.java:3381)
                                                                     at android.app.ActivityThread.performPauseActivity(ActivityThread.java:3340)
                                                                     at android.app.ActivityThread.handleRelaunchActivity(ActivityThread.java:4047)
                                                                     at android.app.ActivityThread.access$1000(ActivityThread.java:150)
                                                                     at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1350)
                                                                     at android.os.Handler.dispatchMessage(Handler.java:102)
                                                                     at android.os.Looper.loop(Looper.java:148)
                                                                     at android.app.ActivityThread.main(ActivityThread.java:5417)
                                                                     at java.lang.reflect.Method.invoke(Native Method)
                                                                     at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726)
                                                                     at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)
                                                                  Caused by: java.lang.SecurityException: Permission denial: writing to settings requires:android.permission.WRITE_SECURE_SETTINGS
                                                                     at android.os.Parcel.readException(Parcel.java:1620)
                                                                     at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:183)
                                                                     at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:135)
                                                                     at android.content.ContentProviderProxy.call(ContentProviderNative.java:646)
                                                                     at android.provider.Settings$NameValueCache.putStringForUser(Settings.java:1322)
                                                                     at android.provider.Settings$Global.putStringForUser(Settings.java:7878)
                                                                     at android.provider.Settings$Global.putString(Settings.java:7862)
                                                                     at android.provider.Settings$Global.putInt(Settings.java:7956)
                                                                     at w.tpbatterylife.MainActivity$override.setAirplaneMode(MainActivity.java:683)
                                                                     at w.tpbatterylife.MainActivity$override.stopTest(MainActivity.java:242)
                                                                     at w.tpbatterylife.MainActivity$override.onPause(MainActivity.java:223)
                                                                     at w.tpbatterylife.MainActivity$override.access$dispatch(MainActivity.java)
                                                                     at w.tpbatterylife.MainActivity.onPause(MainActivity.java:0)
                                                                     at android.app.Activity.performPause(Activity.java:6397)
                                                                     at android.app.Instrumentation.callActivityOnPause(Instrumentation.java:1312)
                                                                     at android.app.ActivityThread.performPauseActivity(ActivityThread.java:3367)
                                                                     at android.app.ActivityThread.performPauseActivity(ActivityThread.java:3340) 
                                                                     at android.app.ActivityThread.handleRelaunchActivity(ActivityThread.java:4047) 
                                                                     at android.app.ActivityThread.access$1000(ActivityThread.java:150) 
                                                                     at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1350) 
                                                                     at android.os.Handler.dispatchMessage(Handler.java:102) 
                                                                     at android.os.Looper.loop(Looper.java:148) 
                                                                     at android.app.ActivityThread.main(ActivityThread.java:5417) 
                                                                     at java.lang.reflect.Method.invoke(Native Method) 
                                                                     at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726) 
                                                                     at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616) 


11-09 19:00:25.903 24597-24597/w.tpbatterylife W/Settings: Setting airplane_mode_on has moved from android.provider.Settings.System to android.provider.Settings.Global, returning read-only value.
11-09 19:00:25.905 24597-24597/w.tpbatterylife D/tpBattery: airplane mode: .........................0
11-09 19:00:34.671 24597-24597/w.tpbatterylife D/tpBattery: set airplane mode: ------------------true
11-09 19:00:34.684 24597-24597/w.tpbatterylife E/AndroidRuntime: FATAL EXCEPTION: main
                                                                 Process: w.tpbatterylife, PID: 24597
                                                                 java.lang.SecurityException: Permission denial: writing to settings requires:android.permission.WRITE_SECURE_SETTINGS
                                                                     at android.os.Parcel.readException(Parcel.java:1620)
                                                                     at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:183)
                                                                     at android.database.DatabaseUtils.readExceptionFromParcel(DatabaseUtils.java:135)
                                                                     at android.content.ContentProviderProxy.call(ContentProviderNative.java:646)
                                                                     at android.provider.Settings$NameValueCache.putStringForUser(Settings.java:1322)
                                                                     at android.provider.Settings$Global.putStringForUser(Settings.java:7878)
                                                                     at android.provider.Settings$Global.putString(Settings.java:7862)
                                                                     at android.provider.Settings$Global.putInt(Settings.java:7956)
                                                                     at w.tpbatterylife.MainActivity.setAirplaneMode(MainActivity.java:683)
                                                                     at w.tpbatterylife.MainActivity.startTest(MainActivity.java:231)
                                                                     at w.tpbatterylife.MainActivity.access$000(MainActivity.java:33)
                                                                     at w.tpbatterylife.MainActivity$1.onClick(MainActivity.java:141)
                                                                     at android.view.View.performClick(View.java:5205)
                                                                     at android.view.View$PerformClick.run(View.java:21164)
                                                                     at android.os.Handler.handleCallback(Handler.java:739)
                                                                     at android.os.Handler.dispatchMessage(Handler.java:95)
                                                                     at android.os.Looper.loop(Looper.java:148)
                                                                     at android.app.ActivityThread.main(ActivityThread.java:5417)
                                                                     at java.lang.reflect.Method.invoke(Native Method)
                                                                     at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:726)
                                                                     at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:616)

C:\Users\bunkk>adb shell pm list permissions -s
All Permissions:

Car information: car vendor channel, car mileage, car fuel level

Contacts: modify your contacts, find accounts on the device, read your contacts

Phone: read call log, read phone status and identity, access IMS call service, directly call phone numbers, write call log, make/receive SIP calls, reroute outgoing calls, add voicemail

Calendar: read calendar events plus confidential information, add or modify calendar events and send email to guests without owners' knowledge

Camera: take pictures and videos

Body Sensors: body sensors (like heart rate monitors), use fingerprint hardware

Location: precise location (GPS and network-based), car speed, approximate location (network-based)

Storage: read the contents of your USB storage, modify or delete the contents of your USB storage

Microphone: record audio

NFC-Near Field communication: NFC Transaction awareness

SMS: read your text messages (SMS or MMS), receive text messages (WAP), receive text messages (MMS), receive text messages (SMS), send and view SMS messages, read cell broadcast messages

ungrouped:
null, Allow STK Intents, null, null, null, null, null, null, modify system settings, null, null, null, connect and disconnect from WiMAX, null, null, null, null, null, null, null, null, internal broadcast, null, null, close other apps, null, null, null

C:\Users\bunkk>

・対策
　permission追加
    <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

実行は問題なく通るのですが、Airplaneモードは変化なし？（アイコンだけで判断できず）

・無線をOFFする
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.UPDATE_DEVICE_STATS "/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>

◇（旧）Settings追加後、onResume内のgetSettingsがあるとonPause/onResume処理繰り返す。
　log_tpBattery_onPause_onResume.txt
  対策：onStartにてgetSettingsを行う

注意事項
-------

　WRITE_SECURE_SETTINGSなどあり、signが必要。
　crash発生した場合、デバッグ時installのSuccessやFailedを確認すること。

その他
-------

◇H-28_Battery情報
　memo_H-28_Battery情報について.txt
　memo_tpBattery.txt

　BatteryManager経由直接電流値の取得は不可

◇残件
　・画面回転対応
　・バッテリー残量0時のログ（ログ無効時）

変更履歴
---------

2018/011/12 v1.0

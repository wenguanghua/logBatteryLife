memo_tpBatteryLife
==================


# 機能

## バッテリ情報を取得
・パワーステタース：
　status,level,plugged,voltage,temperature   ==> broadcast  
・バッテリ情報：
　currentNow,chargeCycle,chargeFull,chargeFullDesign,chargeNow  

## 読取  
一定間隔でバーコードを読取する（初期値は５秒）  

## 記録  
上記情報をlogする：  
　storage>self>primary>Android>data>w.tpbatteryLife>files>logBatteryLife.txt  
・開始するときのlog：  
　・currentNow,chargeCycle,chargeFull,chargeFullDesign,chargeNow  
　・開始時のスクリーンの明るさや読取の有無  
・途中記録内容：count,date,status,level,plugged,voltage,temperature  
・終了時点のlog：終了時刻と経過時間  

## 設定
・バーコードスキャン間隔（初期値：5秒）  
・LogのEnabled/Disableb  
・Loggingのタイミング選択  
・スキャンするとき  
　・定期的  
　・バッテリー状態変化があるとき  
・Logの間隔（default:60秒）  
・reboot後自動起動するかを選択可　＝＞残量0から記録可能に  

## 環境設定
・テスト起動時スクリーン輝度を最小限に設定  
・テスト起動時に飛行モードオン、BluetoothとWiFiもOFF  
　テスト終了時にもとの輝度無線設定に戻す  

## 画面表示
・バッテリー状態を表示  
・APP起動時やテスト開始時にバッテリー情報を表示  
・スキャン間隔情報表示  
・ログ設定情報やログ状態を表示  
・経過時間を表示  

・H-28/H-29画面サイズは違うので、バッテリー情報の表示は左揃い  


# Know-how

## resource参照
　array.xmlは「xml」フォルダではなく、「values」フォルダにいれる

## Fileをチェック
　BroadcastReceiver内でファイルを作成する場合、Fileを生成しているかをチェックする。  
　そうしないとituBroadcastするかわからないので、crash発生する可能性がある。  

## Airplaneモード
    <uses-permission android:name="android.permission.WRITE_SECURE_SETTINGS" />  
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />  
  
　設定する場合、permission追加が必要  

## 無線モード
    <uses-permission android:name="android.permission.BLUETOOTH"/>  
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>  
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>  
    <uses-permission android:name="android.permission.UPDATE_DEVICE_STATS "/>  
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>  
  
  設定する場合、permission追加が必要  


## sign
　WRITE_SECURE_SETTINGSなどあり、signが必要。  
　crash発生した場合、デバッグ時installのSuccessやFailedを確認すること。  

## 電流値
　BatteryManager経由直接電流値の取得は不可

## 残件
　・画面回転対応
　・バッテリー残量0時のログ（ログ無効時）


# 変更履歴
　2018/011/12 v1.0

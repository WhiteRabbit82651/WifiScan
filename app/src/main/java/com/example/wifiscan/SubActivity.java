package com.example.wifiscan;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class SubActivity extends AppCompatActivity {
    private static final String DEBUG_TAG = "★★★ WifiScan Test connect";
    private boolean isAvailable = false;
    private int timeoutMs = 10000; // タイムアウトミリ秒
    private Network network = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sab);

        // インテントからSSIDとタイプとBSSIDを取得する
        Intent intent = getIntent();
        String finalSsid =  intent.getStringExtra("ssid");
        String encryptType = intent.getStringExtra("encryptType");
        String bssid = intent.getStringExtra("bssid");

        // SSIDを表示する
        TextView textView = (TextView) findViewById(R.id.ssid);
        textView.setText(finalSsid);

        // タイプを表示する
        TextView textView2 = (TextView) findViewById(R.id.encryptType);
        textView2.setText(encryptType);

        // BSSIDを表示する
        TextView textView3 = (TextView) findViewById(R.id.bssid);
        textView3.setText(bssid);

        // connectボタン押下時の動作
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View v) {
                // クリック時の処理

                // ソフトキーボードを隠す。
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                EditText editText = (EditText) findViewById(R.id.editTextTextPassword);

                // とりあえずトースト表示
                Toast.makeText(SubActivity.this, "SSID: " + finalSsid + "\ncon" + encryptType + "\nPasswd:" + editText.getText(), Toast.LENGTH_SHORT).show();

                // 登録されている自動のWifi一覧を表示する
                showkList();

                // フリーのWifiスポットであれば、これで接続死に行く？
                // 登録しただけでは、接続詞に行かない。他にトリガーが必要？
//                connect2(finalSsid);

                // アプリが Wi-Fi アクセスポイントに自動接続する端末のネットワーク認証情報を追加する
                // 参照：https://developer.android.com/guide/topics/connectivity/wifi-suggest
                connect(finalSsid, "" + editText.getText());

                // アプリは、リクエストされたネットワークのプロパティを記述する WifiNetworkSpecifier を使用して、端末が接続されているアクセス ポイントを変更するようユーザーに促すことができます。
                // 参照：https://developer.android.com/guide/topics/connectivity/wifi-bootstrap
//                connectOpenNetwork(finalSsid, bssid, ""+editText.getText(), timeoutMs);
            }
        });

        // ADD NETWORK SAGGボタン押下時の動作
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editTextTextPassword);
                String pass = String.valueOf(editText.getText());
                // フリーのWifiスポットであれば、これで接続死に行く？
                // 登録しただけでは、接続詞に行かない。他にトリガーが必要？
                connect2(finalSsid, pass);
            }
        });
        // REMOVE NETWORK SAGGボタン押下時の動作
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editTextTextPassword);
                String pass = String.valueOf(editText.getText());
                // フリーのWifiスポットであれば、これで接続死に行く？
                // 登録しただけでは、接続詞に行かない。他にトリガーが必要？
                disconnect(finalSsid, pass);
            }
        });
    }

    /**
     * アプリが Wi-Fi アクセスポイントに自動接続する端末のネットワーク認証情報を追加する
     * 参照：https://developer.android.com/guide/topics/connectivity/wifi-suggest
     *
     * @param ssid SSID
     * @param password パスワード
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connect(String ssid, String password) {
        final WifiNetworkSuggestion suggestion2 =
                new WifiNetworkSuggestion.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(password)
                        .setIsAppInteractionRequired(true) // Optional (Needs location permission)
                        .build();

        final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<WifiNetworkSuggestion>();
        suggestionsList.add(suggestion2);

        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        final int status = wifiManager.addNetworkSuggestions(suggestionsList);
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            // do error handling here…
            Log.d(DEBUG_TAG, "do error handling here…");
        } else {
            Log.d(DEBUG_TAG, "status is STATUS_NETWORK_SUGGESTIONS_SUCCESS");
        }
        final IntentFilter intentFilter =
                new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(
                        WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return;
                }
                Log.d(DEBUG_TAG, "do post connect processing here...");
                // do post connect processing here...
            }
        };
        this.registerReceiver(broadcastReceiver, intentFilter);
        Log.d(DEBUG_TAG, "cmplate!");
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void showkList(){
        Log.d(DEBUG_TAG, "登録されている自動接続のSSIDをLogcatに表示する");
        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        final List<WifiNetworkSuggestion> suggestionsList = wifiManager.getNetworkSuggestions();

        for(WifiNetworkSuggestion ws :  suggestionsList){
            Log.d(DEBUG_TAG, "SSID=" + ws.getSsid());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connect2(String ssid, String pass) {
        WifiNetworkSuggestion suggestion = null;

        // パスワードあり
        if(pass != null && !pass.equals("")) {
            suggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(pass)
                            .setIsAppInteractionRequired(false) // Optional (Needs location permission)
                            .build();
        }
        // パスワードなし(フリーWifi)
        else {
            suggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setIsAppInteractionRequired(false) // Optional (Needs location permission)
                            .build();
        }

        // リストに追加
        final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<WifiNetworkSuggestion>();
        suggestionsList.add(suggestion);
        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        final int status = wifiManager.addNetworkSuggestions(suggestionsList);

        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            // do error handling here…
            // とりあえずトースト表示
            Toast.makeText(SubActivity.this, "ssid:" + ssid + "\n追加に失敗しました", Toast.LENGTH_SHORT).show();
            Log.d(DEBUG_TAG, "追加に失敗しました");
        } else {
            Toast.makeText(SubActivity.this, "ssid:" + ssid + "\n追加に成功しました", Toast.LENGTH_SHORT).show();
            Log.d(DEBUG_TAG, "追加に成功しました");
        }
        final IntentFilter intentFilter =
                new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(
                        WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return;
                }
                Log.d(DEBUG_TAG, "do post connect processing here...");
                // do post connect processing here...
            }
        };
        this.registerReceiver(broadcastReceiver, intentFilter);
        Log.d(DEBUG_TAG, "cmplate!");
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void disconnect(String ssid, String pass){
        WifiNetworkSuggestion suggestion = null;

        // パスワードあり
        if(pass != null && !pass.equals("")) {
            suggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(pass)
                            .setIsAppInteractionRequired(false) // Optional (Needs location permission)
                            .build();
        }
        // パスワードなし(フリーWifi)
        else {
            suggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setIsAppInteractionRequired(false) // Optional (Needs location permission)
                            .build();
        }

        // リストに追加
        final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<WifiNetworkSuggestion>();
        suggestionsList.add(suggestion);
        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.removeNetworkSuggestions(suggestionsList);

    }

    /**
     * WiFiにピアツーピアでアプリから接続し、接続できたら即切断する。
     * これで端末がWiFiと接続され、画面上部にWiFiのアイコンも表示されるようになる。
     * 切断してWifiにつながるのはたんに自動接続が有効になるから。
     * この部分をコメントアウトしてなんとかつなげたい
     *
     * 参考URL：https://developer.android.com/guide/topics/connectivity/wifi-bootstrap
     *
     * @param ssid
     * @param bssid
     * @param password
     * @param timeoutMs
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectOpenNetwork(String ssid, String bssid, String password, int timeoutMs) {
        final NetworkSpecifier specifier =
                new WifiNetworkSpecifier.Builder()
                        .setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
                        .setBssidPattern(MacAddress.fromString(bssid), MacAddress.fromString("ff:ff:ff:00:00:00"))
                        .setWpa2Passphrase(password)
                        .build();

        final NetworkRequest request =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // このネットワークがインターネットに到達できる必要があることを示します。
//                        .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                        .setNetworkSpecifier(specifier)
                        .build();

        final ConnectivityManager connectivityManager = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);

        final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            /**
             * フレームワークが接続し、使用可能な新しいネットワークを宣言したときに呼び出されます。
             * @param network
             */
            @Override
            public void onAvailable(Network network) {
                // do success processing here..
                Log.d(DEBUG_TAG, "do success processing here..");
                try {
                    URL url = new URL("https://www.google.co.jp");
                    URLConnection uc = network.openConnection(url);

                    Log.d(DEBUG_TAG, "接続出来た・・・？..");

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 接続に成功したら利用可能フラグをTrueにする
                isAvailable = true;
            }

            /**
             * ConnectivityManager.requestNetwork（android.net.NetworkRequest、android.net.ConnectivityManager.NetworkCallback、int）
             * 呼び出しで指定されたタイムアウト時間内にネットワークが見つからない場合、
             * または要求されたネットワーク要求を実行できない場合
             * （タイムアウトが指定されているかどうかに関係なく）に呼び出されます。 ）。
             * このコールバックが呼び出されると、
             * ConnectivityManager.unregisterNetworkCallback（android.net.ConnectivityManager.NetworkCallback）が
             * 呼び出されたかのように、関連するNetworkRequestが既に削除されて解放されます。
             */
            @Override
            public void onUnavailable() {
                // do failure processing here..
                Log.d(DEBUG_TAG, "do failure processing here..");
                isAvailable = false;
            }
        };
        // NetworkCapabilitiesのセットを満たすようにネットワークを要求します。
        connectivityManager.requestNetwork(request, networkCallback, timeoutMs);
        Log.d(DEBUG_TAG, "Release the request when done.");

        // 接続されるのを待つ
        while(isAvailable == false){
            try{
                Thread.sleep(1000);
            } catch(Exception e){

            }
        }
        // NetworkCallbackの登録を解除し
        // requestNetwork（android.net.NetworkRequest、android.net.ConnectivityManager.NetworkCallback）および
        // registerNetworkCallback（android.net.NetworkRequest、android.net.ConnectivityManager.NetworkCallback）
        // 呼び出しから発生したネットワークを解放する
        // アプリが開放することでOSがネットワークに接続するようになる。
        // 自動接続になっていない場合はわからん
        // 仮にWifiに接続されている他機器とパケット通信出来るのであれば、OSがネットワークに接続出来る必要はないかもしれない
        connectivityManager.unregisterNetworkCallback(networkCallback);
        Log.d(DEBUG_TAG, "Release the unregisterNetworkCallback when done.");
        //isAvailable = false;

    }
}

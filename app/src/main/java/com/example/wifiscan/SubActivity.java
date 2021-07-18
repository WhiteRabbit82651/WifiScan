package com.example.wifiscan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.util.Log;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.net.SocketFactory;

public class SubActivity extends AppCompatActivity {
    private static final String DEBUG_TAG = "★★★ WifiScan Test connect";

    private MyNetworkCallback myNetworkCallback = null;
    private static Network myNetwork = null;

    // 外部サーバーのIPアドレス
    private String serverIpAddress = "192.168.0.9";

    private String localIpAddress = "192.168.0.18";

    String finalSsid;
    String encryptType;
    String bssid;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sab);

        // インテントからSSIDとタイプとBSSIDを取得する
        Intent intent = getIntent();
        finalSsid =  intent.getStringExtra("ssid");
        encryptType = intent.getStringExtra("encryptType");
        bssid = intent.getStringExtra("bssid");

        // SSIDを表示する
        TextView textView = (TextView) findViewById(R.id.ssid);
        textView.setText(finalSsid);

        // タイプを表示する
        TextView textView2 = (TextView) findViewById(R.id.encryptType);
        textView2.setText(encryptType);

        // BSSIDを表示する
        TextView textView3 = (TextView) findViewById(R.id.bssid);
        textView3.setText(bssid);

        // ADD NETWORK SUGGESTIONボタン押下時の動作
        findViewById(R.id.button3).setOnClickListener(v -> {
            EditText editText = (EditText) findViewById(R.id.editTextTextPassword);
            String pass = String.valueOf(editText.getText());
            // フリーのWifiスポットであれば、これで接続死に行く？
            // 登録しただけでは、接続詞に行かない。他にトリガーが必要？
            addNetworkSuggestion(finalSsid, pass);
        });
        // REMOVE NETWORK SUGGESTIONボタン押下時の動作
        findViewById(R.id.button4).setOnClickListener(v -> {
            EditText editText = (EditText) findViewById(R.id.editTextTextPassword);
            String pass = String.valueOf(editText.getText());
            // フリーのWifiスポットであれば、これで接続死に行く？
            // 登録しただけでは、接続詞に行かない。他にトリガーが必要？
            removeNetworkSuggestion(finalSsid, pass);
        });
        // SEND PINGボタン押下時の動作
        findViewById(R.id.button2).setOnClickListener(v -> {
            EditText editText = (EditText)findViewById(R.id.editIpAddress);
            String ipAddress = String.valueOf(editText.getText());
            try {
                if(pingForExec(ipAddress)){
                    Toast.makeText(SubActivity.this, ipAddress + "\n到達しました", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SubActivity.this, "到達不可能！！！", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(SubActivity.this, "Exception！！！", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
        // NetworkSpecifierでの接続を試みる
        // 成功した場合、トーストに「利用可能」を表示する
        findViewById(R.id.button5).setOnClickListener(v -> {

            EditText editText = (EditText) findViewById(R.id.editTextTextPassword);
            String pass = String.valueOf(editText.getText());

            connectNetworkSpecifier(pass);
        });

        // NetworkSpecifierでの利用を終了する
        // 成功した場合、トーストに「利用を終了しました」を表示する
        findViewById(R.id.button6).setOnClickListener(v -> disConnectNetworkSpecifier());

        // NetworkSpecifierで接続出来ている場合に、googleに接続してみる
        findViewById(R.id.button).setOnClickListener(v -> {
            ConnectGoogleThread t = new ConnectGoogleThread();
            t.start();
        });

        // NetworkSpecifierで接続出来ている場合に、socketを作成してみる
        findViewById(R.id.button7).setOnClickListener(v -> {
            ConnectSocketThread t = new ConnectSocketThread();
            t.start();
        });

        // サーバーを作成する
        findViewById(R.id.button8).setOnClickListener(v -> {
            StartServerThread t = new StartServerThread();
            t.start();
        });

    }

    /**
     * NetworkSuggestionを追加する
     * @param ssid SSID
     * @param pass SSIDのパスワード
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void addNetworkSuggestion(String ssid, String pass) {
        WifiNetworkSuggestion suggestion;

        // パスワードあり
        if(pass != null && !pass.equals("")) suggestion =
                new WifiNetworkSuggestion.Builder()
                        .setSsid(ssid)
                        .setWpa2Passphrase(pass)
                        .setIsAppInteractionRequired(false) // Optional (Needs location permission)
                        .build();
        // パスワードなし(フリーWifi)
        else {
            suggestion =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setIsAppInteractionRequired(false) // Optional (Needs location permission)
                            .build();
        }

        // リストに追加
        final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
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

    /**
     * NetworkSuggestionを削除する
     * @param ssid SSID
     * @param pass SSIDのパスワード
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void removeNetworkSuggestion(String ssid, String pass){
        WifiNetworkSuggestion suggestion;

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
        final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
        suggestionsList.add(suggestion);
        final WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        // アプリが追加したNetworkSuggestionsを削除する
        wifiManager.removeNetworkSuggestions(suggestionsList);

        Toast.makeText(SubActivity.this, "SSIDをリストから削除しました", Toast.LENGTH_SHORT).show();

    }

    /**
     * adb shell で実行可能なping コマンドを実行することでネットワークの疎通確認を行います
     *
     * @return 接続可能ならtrue, 不可能ならfalse
     */
    private boolean pingForExec(String ipAddress){
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        try{
            proc = runtime.exec("ping -c 5 " + ipAddress);
            proc.waitFor();
        }catch(Exception ignored){}
        int exitVal = Objects.requireNonNull(proc).exitValue();
        return exitVal == 0;
    }

    /**
     * NetworkSpecifierで接続する
     * @param passwd 接続するSSIDのパスワード
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectNetworkSpecifier(String passwd){

        // NetworkSpecifier(ネットワークスペシファイアー)オブジェクトを作成する
        // NetworkSpecifierはNetworkRequestで使用する、要求されたネットワークの特定のプロパティを記述します。
        final NetworkSpecifier specifier =
                new WifiNetworkSpecifier.Builder()
                        .setSsidPattern(new PatternMatcher(finalSsid, PatternMatcher.PATTERN_PREFIX))
                        .setBssidPattern(MacAddress.fromString(bssid), MacAddress.fromString("ff:ff:ff:00:00:00"))
                        .setWpa2Passphrase(passwd)
                        .build();

        // NetworkRequestを作成する
        // NetworkRequestはNetworkRequest.Builderを介して行われるネットワークの要求を定義し、
        // ConnectivityManager#requestNetworkを介してネットワークを要求したり、
        // ConnectivityManager#registerNetworkCallbackを介して変更をリッスンするために使用されます。
        final NetworkRequest request =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .setNetworkSpecifier(specifier)
                        .build();

        // ConnectivityManagerを作成する
        // ConnectivityManagerはネットワークの接続状態についての問い合わせに答えるクラス。
        // また、ネットワークの接続性が変化した際には、アプリケーションに通知します。
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        // 接続リクエスト
        myNetworkCallback = new MyNetworkCallback();
        connectivityManager.requestNetwork(request, myNetworkCallback);
    }

    /**
     * NetworkSpecifierで接続したネットワークを開放する
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void disConnectNetworkSpecifier(){
        if(myNetworkCallback != null) {
            // ConnectivityManagerを作成する
            // ConnectivityManagerはネットワークの接続状態についての問い合わせに答えるクラス。
            // また、ネットワークの接続性が変化した際には、アプリケーションに通知します。
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

            connectivityManager.unregisterNetworkCallback(myNetworkCallback);
            myNetworkCallback = null;
            myNetwork = null;
            Toast.makeText(SubActivity.this, "ネットワークを開放しました", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(SubActivity.this, "何かしらNULLのため終了処理を受付出来ませんでした", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectGoogle() {

        try {
            if (myNetwork != null) {
                URL url = new URL("https://www.google.co.jp/");
                URLConnection urlConnection = myNetwork.openConnection(url);
                String contentType = urlConnection.getHeaderField("Content-Type");

                Toast.makeText(SubActivity.this, contentType, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SubActivity.this, "Network Specifierで接続されていません", Toast.LENGTH_SHORT).show();
            }
        } catch(Exception e){
            Log.d(DEBUG_TAG, "あばばばばばば");
            e.printStackTrace();
        }
    }

    /**
     * サーバーに接続してメッセージを受け取る
     *
     */
    private void connectSocket(){
        try {
            if (myNetwork != null) {

                // 保持しているNetworkを利用してソケットを作成
                SocketFactory socketFactory = myNetwork.getSocketFactory();

                // ソケットを作成し、connectまで行ってくれる(connectすると、相手がaccept()されて、サーバー側のaccept以降の処理が実施される)
                Socket socket = socketFactory.createSocket(serverIpAddress,8080);

                // 接続したらメッセージを受け取る
                byte[] data = new byte[1024];
                InputStream in = socket.getInputStream();
                int readSize = in.read(data);
                data = Arrays.copyOf(data, readSize);
                String readStr = new String(data, StandardCharsets.UTF_8);
                System.out.println("「"+readStr+"」を受信しました。");
                Log.d(DEBUG_TAG, readStr);

                // 使い終わったらクローズする
                // サーバー側はaccept次第sendでメッセージを送ってcloseしているので、これ以上このソケットは使えない
                in.close();

                // ソケットを閉じる
                // 閉じてしまっていいのか謎
                socket.close();

            } else {
                Toast.makeText(SubActivity.this, "Network Specifierで接続されていません", Toast.LENGTH_SHORT).show();
            }
        } catch(Exception e){
            Log.d(DEBUG_TAG, "あばばばばばば");
            e.printStackTrace();
        }
    }

    /**
     * メッセージを送信するサーバー
     */
    private void server(){
        try {
            ServerSocket server = new ServerSocket();
            server.bind(new InetSocketAddress(localIpAddress, 8080));

            Socket socket = server.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            // クライアントへメッセージを送信する
            writer.println("Hello My Name is Android.");
            writer.close();
            reader.close();
            socket.close();
            Log.d(DEBUG_TAG, "クライアントからの接続を受け付けました");

            System.out.println("クライアントへメッセージを送信");
            Log.d(DEBUG_TAG, "クライアントへメッセージを送信");
        } catch(Exception e){
            Log.d(DEBUG_TAG, "あばばばばばば");
            e.printStackTrace();
        }
    }

    class ConnectGoogleThread extends Thread {
        public void run(){
            connectGoogle();
        }
    }

    class ConnectSocketThread extends Thread {
        public void run(){
            connectSocket();
        }
    }

    class StartServerThread extends Thread {
        public void run(){
            server();
        }
    }

    class MyNetworkCallback extends ConnectivityManager.NetworkCallback {

        /**
         * アベイラブル 利用出来る
         * @param network Network
         */
        @Override
        public void onAvailable(Network network) {
            // do success processing here..
            Toast.makeText(SubActivity.this, "利用可能", Toast.LENGTH_SHORT).show();
            myNetwork = network;
        }

        @Override
        public void onUnavailable() {
            // do failure processing here..
            Toast.makeText(SubActivity.this, "利用を終了しました", Toast.LENGTH_SHORT).show();
        }
    }
}

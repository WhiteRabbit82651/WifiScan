package com.example.wifiscan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    WifiManager wifiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("WifiScan", "start WifiScan");

        try {
            wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    boolean success = intent.getBooleanExtra(
                            WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (success) {
                        scanSuccess();
                    } else {
                        // scan failure handling
                        scanFailure();
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            this.registerReceiver(wifiScanReceiver, intentFilter);

            boolean success = wifiManager.startScan();
            if (!success) {
                // scan failure handling
                scanFailure();
            }
        } catch (Exception e) {
            Log.e("WifiScan", e.getMessage());
        }
    }

    private void scanSuccess() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        Log.d("WifiScan","configuredNetworks size = " + configuredNetworks.size());

        List<ScanResult> results = wifiManager.getScanResults();
        ArrayList<String> list = new ArrayList<>();
        for (ScanResult s: results) {
            Log.d("WifiScan",s.SSID);

            String encryptType = "";
            if (s.capabilities.contains("WPA2")) {
                encryptType = "WPA2";
            }
            else if (s.capabilities.contains("WPA")) {
                encryptType = "WPA";
            }
            else if (s.capabilities.contains("WEP")) {
                encryptType = "WEP";
            }

            list.add(s.SSID + "_" + encryptType + "_" + s.BSSID);
        }
        // リスト項目とListViewを対応付けるArrayAdapterを用意する
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);

        // ListViewにArrayAdapterを設定する
        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(adapter);
        //リスト項目クリック時の処理
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // ここに処理を記述します。
            //今回は、トースト表示
            ListView listView1 =(ListView)parent;
            String item=(String) listView1.getItemAtPosition(position);
            //Toast.makeText(MainActivity.this, "Click: "+item, Toast.LENGTH_SHORT).show();

            // サブ画面へ遷移
            Intent intent = new Intent(getApplication(), SubActivity.class);
            intent.putExtra("ssid", item.split("_")[0]);
            intent.putExtra("encryptType", item.split("_")[1]);
            intent.putExtra("bssid", item.split("_")[2]);
            startActivity(intent);
        });
    }
    private void scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult s: results) {
            Log.d("WifiScan",s.SSID);
        }
    }
}
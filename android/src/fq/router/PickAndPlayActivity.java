package fq.router;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import fq.router.utils.HttpUtils;
import fq.router.utils.IOUtils;

import java.util.ArrayList;
import java.util.HashMap;

public class PickAndPlayActivity extends ListActivity {
    private android.os.Handler handler = new android.os.Handler();
    private ArrayAdapter arrayAdapter;
    private static final ArrayList<String> items = new ArrayList<String>();
    private static final HashMap<String, String> macAddresses = new HashMap<String, String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_and_play);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, items);
        setListAdapter(arrayAdapter);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateStatus("scanning...");
                    HttpUtils.get("http://127.0.0.1:8318/lan/scan", new IOUtils.Callback() {
                        @Override
                        public void onLineRead(String line) {
                            try {
                                String[] parts = line.split(",", -1);
                                if (3 == parts.length) {
                                    onNewDeviceDiscovered(parts[0], parts[1], parts[2]);
                                } else {
                                    Log.e("fqrouter", "unexpected line[" + parts.length + "]: " + line);
                                }
                            } catch (Exception e) {
                                Log.e("fqrouter", "failed to read line", e);
                            }
                        }
                    }, 0);
                    updateStatus("scan completed!");
                } catch (Exception e) {
                    updateStatus("scan failed");
                    Log.e("fqrouter", "failed to scan lan", e);
                }
            }
        }).start();
    }

    private void updateStatus(final String text) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView statusTextBox = (TextView) findViewById(R.id.statusTextBox);
                statusTextBox.setText(text);
            }
        }, 0);

    }

    private void onNewDeviceDiscovered(final String ip, final String macAddress, final String hostName) {
        updateStatus("scanning " + ip + "...");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!macAddress.isEmpty()) {
                    macAddresses.put(ip, macAddress);
                    items.add(ip + " - " + hostName);
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }, 0);
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
    }
}
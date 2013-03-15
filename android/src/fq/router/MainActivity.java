package fq.router;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import fq.router.utils.ShellUtils;


public class MainActivity extends Activity implements StatusUpdater {
    private Handler handler = new Handler();
    private boolean logs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setupUI();
    }

    private void setupUI() {
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        Button manageButton = (Button) findViewById(R.id.manageButton);
        manageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8888")));
            }
        });
        Button exitButton = (Button) findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ManagerProcess.kill();
                } catch (Exception e) {
                    Log.e("fqrouter", "failed to kill manager process", e);
                }
                finish();
            }
        });
        final Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startButton.setText("Starting...");
                startButton.setEnabled(false);
                appendLog("starting supervisor thread");
                new Thread(new Supervisor(getAssets(), MainActivity.this)).start();
            }
        });
        final Button reportErrorButton = (Button) findViewById(R.id.reportErrorButton);
        reportErrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"fqrouter@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "android fqrouter error report for version " + getMyVersion());
                String body;
                try {
                    body = getErrorMailBody();
                } catch (Exception e) {
                    Log.e("fqrouter", "failed to get error mail body", e);
                    body = "";
                }
                i.putExtra(Intent.EXTRA_TEXT, body);
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void updateStatus(final String status) {
        appendLog("status updated to: " + status);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.statusTextView);
                textView.setText(status);
            }
        }, 0);
    }

    public void appendLog(final String log) {
        Log.i("fqrouter", log);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.logTextView);
                textView.setText(log + "\n" + textView.getText());
            }
        }, 0);
    }

    @Override
    public void activateManageButton() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.startButton).setVisibility(View.GONE);
                findViewById(R.id.reportErrorButton).setVisibility(View.VISIBLE);
                findViewById(R.id.manageButton).setVisibility(View.VISIBLE);
            }
        }, 0);
        updateStatus("Ready! manage button activated");
    }

    @Override
    public void reportError(final String msg, Exception e) {
        if (null == e) {
            Log.e("fqrouter", msg);
        } else {
            Log.e("fqrouter", msg, e);
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.startButton).setVisibility(View.GONE);
                findViewById(R.id.reportErrorButton).setVisibility(View.VISIBLE);
            }
        }, 0);
        updateStatus("Error: " + msg);
    }

    public String getErrorMailBody() throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("phone model: " + Build.MODEL + "\n");
        body.append("android version: " + Build.VERSION.RELEASE + "\n");
        body.append("kernel version: " + System.getProperty("os.version") + "\n");
        body.append("fqrouter version: " + getMyVersion() + "\n");
        body.append(ShellUtils.sudo(false, "/system/bin/logcat", "-d", "-v", "time", "-s", "fqrouter:V"));
        return body.toString();
    }

    public String getMyVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            Log.e("fqrouter", "failed to get package info", e);
            return "Unknown";
        }
    }
}

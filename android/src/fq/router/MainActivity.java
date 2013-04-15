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
import android.widget.TextView;
import android.widget.Toast;
import fq.router.utils.IOUtils;
import fq.router.utils.ShellUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


public class MainActivity extends Activity implements StatusUpdater {
    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setupUI();
        new Thread(new Runnable() {
            @Override
            public void run() {
                appendLog("ver: " + getMyVersion());
                if (Supervisor.ping()) {
                    appendLog("found manager is already running");
                    activateManageButton();
                }
            }
        }).start();
    }

    private void setupUI() {
        TextView textView = (TextView) findViewById(R.id.logTextView);
        textView.setMovementMethod(new ScrollingMovementMethod());
        Button manageButton = (Button) findViewById(R.id.manageButton);
        manageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8318")));
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
                Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"fqrouter@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "android fqrouter error report for version " + getMyVersion());
                i.putExtra(Intent.EXTRA_TEXT, getErrorMailBody());
                createLogFiles();
                attachLogFiles(i, "/sdcard/manager.log", "/sdcard/logcat.log",
                        "/sdcard/getprop.log", "/sdcard/dmesg.log");
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

    public void activateManageButton() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.startButton).setVisibility(View.GONE);
                findViewById(R.id.manageButton).setVisibility(View.VISIBLE);
            }
        }, 0);
        updateStatus("Press manage button to roll");
    }

    @Override
    public void activateAndClickManageButton() {
        activateManageButton();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.manageButton).performClick();
            }
        }, 0);
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
            }
        }, 0);
        updateStatus("Error: " + msg);
    }

    private String getErrorMailBody() {
        StringBuilder body = new StringBuilder();
        body.append("phone model: " + Build.MODEL + "\n");
        body.append("android version: " + Build.VERSION.RELEASE + "\n");
        body.append("kernel version: " + System.getProperty("os.version") + "\n");
        body.append("fqrouter version: " + getMyVersion() + "\n");
        return body.toString();
    }

    private String getMyVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            Log.e("fqrouter", "failed to get package info", e);
            return "Unknown";
        }
    }

    private void createLogFiles() {
        try {
            deployCaptureLogSh();
            ShellUtils.sudo("/system/bin/sh", "/data/data/fq.router/capture-log.sh");
        } catch (Exception e) {
            Log.e("fqrouter", "failed to execute capture-log.sh", e);
            try {
                ShellUtils.sudo(false, "/system/bin/getprop", ">", "/sdcard/getprop.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute getprop", e2);
            }
            try {
                ShellUtils.sudo(false, "/system/bin/dmesg", ">", "/sdcard/dmesg.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute dmesg", e2);
            }
            try {
                ShellUtils.sudo(false, "/system/bin/logcat", "-d", "-v", "time", "-s", "fqrouter:V", ">", "/sdcard/logcat.log");
            } catch (Exception e2) {
                Log.e("fqrouter", "failed to execute logcat", e2);
            }
        }
    }

    private void deployCaptureLogSh() {
        try {
            InputStream inputStream = getAssets().open("capture-log.sh");
            try {
                OutputStream outputStream = new FileOutputStream("/data/data/fq.router/capture-log.sh");
                try {
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            Log.e("fqrouter", "failed to deploy capture-log.sh", e);
        }
    }

    private void attachLogFiles(Intent i, String... logFilePaths) {
        ArrayList<Uri> logFiles = new ArrayList<Uri>();
        for (String logFilePath : logFilePaths) {
            File logFile = new File(logFilePath);
            if (logFile.exists()) {
                logFiles.add(Uri.fromFile(logFile));
            }
        }
        try {
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, logFiles);
        } catch (Exception e) {
            Log.e("fqrouter", "failed to attach log", e);
        }
    }
}

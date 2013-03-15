package fq.router;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements StatusUpdater {
    private Handler handler = new Handler();

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
        Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appendLog("starting supervisor thread");
                new Thread(new Supervisor(getAssets(), MainActivity.this)).start();
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
                findViewById(R.id.manageButton).setVisibility(View.VISIBLE);
            }
        }, 0);
        updateStatus("Ready! manage button activated");
    }
}

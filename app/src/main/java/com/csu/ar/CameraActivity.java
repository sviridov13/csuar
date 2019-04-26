package com.csu.ar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import cn.easyar.Engine;
import com.csu.ar.R;

public class CameraActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    private static final String key = "rlJ5QRvzfNBpbNvIAsjzwNpixMD0TsFAQ50UAb7kHGpksdiItGSuTwPMgvUsEVTI8nmEpKs2kEnxLk7TpslJ75DDBEVby8gEBRbjabpDmvRQpnLJis2WaLQx21RXkqIfwl7lTzuBs7x8S40DH3XCv3mt4u31Z4lqKhj2JMOYWFdPzkxm9mFWh3kfhpN2iX2AZJyQ9Grf";

    private GLView GLViewSurface;
    private FrameLayout ARView;
    private BroadcastReceiver broadcastReceiver;

    private ImageView cameraCaptureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        if (!Engine.initialize(this, key)) {
            Log.e(TAG, "Initialization Failed.");
        }

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Intent received");
                String status = intent.getStringExtra("Status");
                if (status.equals("Created")) {
                    Log.i(TAG, "Start PreviewImageActivity");
                   preparationAndOpenPreviewImageActivity();
                } else if (status.equals("Fail")) {
                    Log.e(TAG, "Failed to save and open image");
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("pictureAvailability"));

        ARView = (FrameLayout) findViewById(R.id.ARView);

        cameraCaptureButton = (ImageView) findViewById(R.id.cameraPick);

        cameraCaptureButton.setOnClickListener(clickListener);

        GLViewSurface = new GLView(getApplicationContext());
        ARView.addView(GLViewSurface, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Click on cameraCaptureButton button");
            GLViewSurface.setScreenshot(true);
            GLViewSurface.onPause();

        }
    };

    public void switchCameraAction() {
        GLViewSurface.onPause();
        GLViewSurface.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GLViewSurface.onResume();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    public void preparationAndOpenPreviewImageActivity() {
        Intent startActivityIntent = new Intent(this, PreviewImageActivity.class);
        startActivity(startActivityIntent);
    }

}

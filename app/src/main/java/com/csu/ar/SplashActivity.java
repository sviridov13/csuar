package com.csu.ar;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import cn.easyar.samples.helloar.R;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

}

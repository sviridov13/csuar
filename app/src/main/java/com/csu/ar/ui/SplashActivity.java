package com.csu.ar.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import com.csu.ar.R;
import com.csu.ar.services.LoadFilesService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import java.util.HashMap;

public class SplashActivity extends Activity {

    private final String TAG = this.getClass().getSimpleName();

    private static final String LOAD_FILES_ACTION = "com.csu.ar.action.LOAD_FILES_ACTION";

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Intent intent = new Intent(this, LoadFilesService.class);
        intent.setAction(LOAD_FILES_ACTION);

        requestCameraPermission(new SplashActivity.PermissionCallback() {
            @Override
            public void onSuccess() {
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    startService(intent);
                } else {
                    signInAnonymously();
                }
            }

            @Override
            public void onFailure() {
            }
        });
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Intent intent = new Intent(SplashActivity.this, LoadFilesService.class);
                intent.setAction(LOAD_FILES_ACTION);
                SplashActivity.this.startService(intent);
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e(TAG, "signInAnonymously:FAILURE", exception);
                    }
                });
    }


    private interface PermissionCallback {
        void onSuccess();

        void onFailure();
    }

    private HashMap<Integer, SplashActivity.PermissionCallback> permissionCallbacks = new HashMap<Integer, SplashActivity.PermissionCallback>();
    private int permissionRequestCodeSerial = 0;

    @TargetApi(23)
    private void requestCameraPermission(SplashActivity.PermissionCallback callback) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                int requestCode = permissionRequestCodeSerial;
                permissionRequestCodeSerial += 1;
                permissionCallbacks.put(requestCode, callback);
                requestPermissions(new String[]{Manifest.permission.CAMERA}, requestCode);
            } else {
                callback.onSuccess();
            }
        } else {
            callback.onSuccess();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionCallbacks.containsKey(requestCode)) {
            SplashActivity.PermissionCallback callback = permissionCallbacks.get(requestCode);
            permissionCallbacks.remove(requestCode);
            boolean executed = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    executed = true;
                    callback.onFailure();
                }
            }
            if (!executed) {
                callback.onSuccess();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

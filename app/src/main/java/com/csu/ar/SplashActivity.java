package com.csu.ar;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import com.csu.ar.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.HashMap;

public class SplashActivity extends Activity {

    private final String TAG = this.getClass().getSimpleName();

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    FirebaseAuth mAuth;

    String tmpDir = System.getProperty("java.io.tmpdir");
    File targetsDir = new File(tmpDir + "/targets");
    File modelsDir = new File(tmpDir + "/models");
    File backgroundsDir = new File(tmpDir + "/backgrounds");
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        db = FirebaseFirestore.getInstance();

        requestCameraPermission(new SplashActivity.PermissionCallback() {
            @Override
            public void onSuccess() {
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    loadFilesToCache();
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
                loadFilesToCache();
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e(TAG, "signInAnonymously:FAILURE", exception);
                    }
                });
    }

    private void startMainActivity() {
        if (!CameraActivity.getStatus()) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
            Log.i(TAG, "CameraActivity start");
            finish();
        } else {
            Log.i(TAG, "CameraActivity already start");
        }
    }

    private void loadModels() {
        if (!modelsDir.exists()) {
            modelsDir.mkdir();
        }

        db.collection("models")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String fullname = document.getId();
                                StorageReference modelRef = storageRef.child("models/" + fullname);
                                File localFile = new File(modelsDir, fullname);
                                Log.i(TAG, localFile.getAbsolutePath());
                                if (!localFile.exists()) {
                                    modelRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            Log.i(TAG, "Start from backs");
                                            loadBackgrounds();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            Log.e(TAG, exception.getMessage());
                                        }
                                    });
                                } else {
                                    loadBackgrounds();
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void loadFilesToCache() {
        storage.setMaxDownloadRetryTimeMillis(1);
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        // загрузка целей
        if (!targetsDir.exists()) {
            targetsDir.mkdir();
        }
        db.collection("targets")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String fullname = document.getId();
                                Log.i(TAG, fullname);
                                StorageReference modelRef = storageRef.child("targets/" + fullname);
                                File localFile = new File(targetsDir, fullname);
                                if (!localFile.exists()) {
                                    Log.e(TAG, localFile.getAbsolutePath());
                                    modelRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            Log.i(TAG, "Load Models");
                                            loadModels();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            Log.e(TAG, exception.getMessage());
                                        }
                                    });
                                } else {
                                    Log.i(TAG, "Load Models 2");
                                    loadModels();
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

    private void loadBackgrounds() {
        if(!backgroundsDir.exists()) {
            backgroundsDir.mkdir();
        }
        db.collection("backgrounds")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String fullname = document.getId();
                                StorageReference backgroundRef = storageRef.child("backgrounds/" + fullname);
                                File localFile = new File(backgroundsDir, fullname);
                                if (!localFile.exists())
                                    backgroundRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            Log.i(TAG, "Start from backgrounds");
                                            startMainActivity();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {

                                        }
                                    });
                                else {
                                    startMainActivity();
                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
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

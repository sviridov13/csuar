package com.csu.ar;

import android.content.Intent;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class SplashActivity extends Activity {

    private final String TAG = this.getClass().getSimpleName();

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();


    String tmpDir = System.getProperty("java.io.tmpdir");
    File targetsDir = new File(tmpDir + "/targets");
    File modelsDir = new File(tmpDir + "/models");
    File backgroundsDir = new File(tmpDir + "/backgrounds");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        loadFilesToCache();
        //startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadFilesToCache() {
        storage.setMaxDownloadRetryTimeMillis(1);
        FirebaseFirestore db = FirebaseFirestore.getInstance();


        // загрузка целей
        if(!targetsDir.exists()) {
            targetsDir.mkdir();
        }
        StorageReference targetRef = storageRef.child("namecard.jpg");
        File localFile = new File(targetsDir, "namecard.jpg");
        if (!localFile.exists())
            targetRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Log.i(TAG, "Start from targets");
                    startMainActivity();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                }
            });
//        db.collection("targets")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                String fullname = document.getId();
//                                StorageReference targetRef = storageRef.child("targets/" + fullname);
//                                File localFile = new File(targetsDir, fullname);
//                                if (!localFile.exists())
//                                    targetRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                                        @Override
//                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                                            Log.i(TAG, "Start from targets");
//                                            startMainActivity();
//                                        }
//                                    }).addOnFailureListener(new OnFailureListener() {
//                                        @Override
//                                        public void onFailure(@NonNull Exception exception) {
//
//                                        }
//                                    });
//                            }
//                        } else {
//                            Log.d(TAG, "Error getting documents: ", task.getException());
//                        }
//                    }
//                });


//        // загрузка моделей
//        if(!modelsDir.exists()) {
//            modelsDir.mkdir();
//        }
//        db.collection("models")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                String fullname = document.getId();
//                                StorageReference modelRef = storageRef.child(fullname);
//                                File localFile = new File(modelsDir, fullname);
//                                if (!localFile.exists())
//                                    modelRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                                        @Override
//                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                                            Log.i(TAG, "Start from models");
//                                            startMainActivity();
//                                        }
//                                    }).addOnFailureListener(new OnFailureListener() {
//                                        @Override
//                                        public void onFailure(@NonNull Exception exception) {
//                                        }
//                                    });
//                            }
//                        } else {
//                            Log.d(TAG, "Error getting documents: ", task.getException());
//                        }
//                    }
//                });
//
//
//        // загрузка бэкграундов
//        if(!backgroundsDir.exists()) {
//            backgroundsDir.mkdir();
//        }
//        db.collection("backgrounds")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                String fullname = document.getId();
//                                StorageReference backgroundRef = storageRef.child("backgrounds/" + fullname);
//                                File localFile = new File(backgroundsDir, fullname);
//                                if (!localFile.exists())
//                                    backgroundRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                                        @Override
//                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                                            Log.i(TAG, "Start from backgrounds");
//                                            startMainActivity();
//                                        }
//                                    }).addOnFailureListener(new OnFailureListener() {
//                                        @Override
//                                        public void onFailure(@NonNull Exception exception) {
//
//                                        }
//                                    });
//                            }
//                        } else {
//                            Log.d(TAG, "Error getting documents: ", task.getException());
//                        }
//                    }
//                });



    }

}

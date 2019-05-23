package com.csu.ar.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import com.csu.ar.ui.CameraActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class LoadFilesService extends IntentService {

    private static final String TAG = LoadFilesService.class.getSimpleName();

    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReference();

    FirebaseAuth mAuth;

    String tmpDir = System.getProperty("java.io.tmpdir");
    File targetsDir = new File(tmpDir + "/targets");
    File modelsDir = new File(tmpDir + "/models");
    File backgroundsDir = new File(tmpDir + "/backgrounds");
    private FirebaseFirestore db;

    public LoadFilesService() {
        super("LoadFilesService");
    }
    @Override
    public void onHandleIntent(Intent intent) {
        db = FirebaseFirestore.getInstance();
        loadFilesToCache();
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
                                            startCameraActivity();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {

                                        }
                                    });
                                else {
                                    startCameraActivity();
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

    private void startCameraActivity() {
        if (!CameraActivity.getStatus()) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
            Log.i(TAG, "CameraActivity start");
        } else {
            Log.i(TAG, "CameraActivity already start");
        }
    }
}

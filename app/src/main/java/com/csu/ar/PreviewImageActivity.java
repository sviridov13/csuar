package com.csu.ar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.csu.ar.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PreviewImageActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    int WRITE_EXTERNAL_STORAGE = 1;

    ImageView previewImage;
    ProgressBar progressBar;
    RelativeLayout saveNavigationButton;
    RelativeLayout shareNavigationButton;
    Bitmap bitmap;
    Bitmap defaultBitmap;
    Uri uri;

    ArrayList<String> backgroundsList;
    Bitmap bg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        backgroundsList = new ArrayList<>();
        backgroundsList.add("file:///android_asset/default_pic_pic.png");
        File backgroundsDir = new File(System.getProperty("java.io.tmpdir") + "/backgrounds");
        if (backgroundsDir.exists()) {
            File[] backgroundsFiles = backgroundsDir.listFiles();
            for (File backgroundsFile : backgroundsFiles) {
                backgroundsList.add(backgroundsDir + "/" + backgroundsFile.getName());
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }


        setContentView(R.layout.activity_preview_image);

        previewImage = (ImageView) findViewById(R.id.previewImage);
        setImageView();

        progressBar = (ProgressBar) findViewById(R.id.previewProgress);

        saveNavigationButton = (RelativeLayout) findViewById(R.id.saveNavigationButton);
        shareNavigationButton = (RelativeLayout) findViewById(R.id.shareNavigationButton);

        saveNavigationButton.setOnClickListener(onClickListener);
        shareNavigationButton.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.saveNavigationButton:
                    if (ContextCompat.checkSelfPermission(PreviewImageActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                        saveFileToDeviceMemory();
                    else
                        requestCameraPermission();
                    break;
                case R.id.shareNavigationButton:
                    openShareMenu();
                    break;
            }
        }
    };

    private void openShareMenu() {
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("image/jpeg")
                .setStream(uri)
                .setText("#csu #компьютернаябезопастность")
                .setChooserTitle("Поделиться с помощью приложения:")
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivity(shareIntent);
    }

    private void updateUri(Bitmap nBitmap) {
        try {
            File file = new File(getCacheDir(), "PreviewImage.png");
            FileOutputStream fOut = new FileOutputStream(file);
            nBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, false);
            uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

            Log.i(TAG, "Successfully save preview image to cache");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save preview image to cache" + e.getMessage());
        }
    }

    private void saveFileToDeviceMemory() {
        try {
            String snackBarStr;
            String photoTimeStamp = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss",
                    Locale.US).format(new Date());

            File photoFile = new File(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    String.format("IS_%s.jpeg", photoTimeStamp));

            FileOutputStream fos = new FileOutputStream(photoFile);

            boolean bo = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Log.i(TAG, "Photo saved: " + bo);
            if (bo)
                snackBarStr = "Фото успешно сохранено";
            else
                snackBarStr = "Не удалось сохранить фото";
            Snackbar.make(findViewById(R.id.imageBody), snackBarStr, Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save image " + e.getMessage());
        }
    }

    private void setImageView() {
        try {
            bitmap = BitmapFactory.decodeStream(openFileInput("tempImage.jpeg"));
            previewImage.setImageBitmap(bitmap);

            updateUri(bitmap);

            Log.i(TAG, "Successfully set preview image");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set preview image " + e.getMessage());
            onDestroy();
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(PreviewImageActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            saveFileToDeviceMemory();
        } else {
            Snackbar.make(findViewById(R.id.imageBody), "Необходим доступ к памяти устройства", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}

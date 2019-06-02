package com.csu.ar.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import com.csu.ar.BackgroundListAdapter;
import com.dailystudio.app.utils.BitmapUtils;
import com.dailystudio.development.Logger;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.FieldValue;
import com.csu.ar.segmentation.DeeplabInterface;
import com.csu.ar.segmentation.DeeplabModel;
import com.csu.ar.segmentation.ImageUtils;
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

    private static final int WRITE_EXTERNAL_STORAGE = 1;

    private ImageView previewImage;
    private ProgressBar progressBar;
    private RelativeLayout saveNavigationButton;
    private RelativeLayout shareNavigationButton;
    private Bitmap bitmap;
    private Bitmap defaultBitmap;
    private Bitmap finallyCropped;
    private Uri uri;

    private ArrayList<String> backgroundsList;
    private Bitmap bg;

    private class InitializeModelAsyncTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            final boolean ret = DeeplabModel.getInstance().initialize(getApplicationContext());
            Log.i(TAG, "initialize deeplab model: " + ret);
            return ret;
        }
    }

    private void initModel() {
        try {
            new InitializeModelAsyncTask().execute((Void) null);
        } catch (Exception e) {
            Log.e(TAG, "Init model: " + e.getMessage());
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        backgroundsList = new ArrayList<>();
        // Дефолтное изображение
        backgroundsList.add("file:///android_asset/delete_background.png");
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

        initModel();

        setContentView(R.layout.activity_preview_image);

        initRecyclerView();

        previewImage = (ImageView) findViewById(R.id.previewImage);
        setImageView();

        progressBar = (ProgressBar) findViewById(R.id.previewProgress);

        saveNavigationButton = (RelativeLayout) findViewById(R.id.saveNavigationButton);
        shareNavigationButton = (RelativeLayout) findViewById(R.id.shareNavigationButton);

        saveNavigationButton.setOnClickListener(onClickListener);
        shareNavigationButton.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
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

    private class ThreadSegmentation extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            return segmentImage();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Bitmap mBitmap) {
            super.onPostExecute(bitmap);
            if (mBitmap != null) {
                previewImage.setImageBitmap(mBitmap);
                updateUri(mBitmap);
                bitmap = mBitmap;
            }
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private Bitmap segmentImage() {
        try {
            DeeplabInterface deeplabInterface = DeeplabModel.getInstance();

            if (!deeplabInterface.isInitialized()) {
                initModel();
            }

            if (defaultBitmap == null) { defaultBitmap = bitmap; }
            else { bitmap = defaultBitmap; }

            if (bg == null) { return defaultBitmap; }

            final int w = bitmap.getWidth();
            final int h = bitmap.getHeight();

            if (finallyCropped == null) {
                float resizeRatio = (float) deeplabInterface.getInputSize() / Math.max(bitmap.getWidth(), bitmap.getHeight());
                int rw = Math.round(w * resizeRatio);
                int rh = Math.round(h * resizeRatio);

                Logger.debug("Resize bitmap: ratio = %f, [%d x %d] -> [%d x %d]",
                        resizeRatio, w, h, rw, rh);

                Bitmap resized = ImageUtils.tfResizeBilinear(bitmap, rw, rh);
                Bitmap mask = deeplabInterface.segment(resized);

                if (mask != null) {
                    mask = BitmapUtils.createClippedBitmap(mask,
                            (mask.getWidth() - rw) / 2,
                            (mask.getHeight() - rh) / 2,
                            rw, rh);
                    mask = BitmapUtils.scaleBitmap(mask, w, h);
                }

                finallyCropped = cropBitmapWithMask(bitmap, mask);
            }

            bg = ImageUtils.tfResizeBilinear(bg, w, h);
            return cropBitmapWithBG(finallyCropped, bg);
        } catch (Exception e) {
            Log.e(TAG, "Some error when segment " + e.getMessage());
            return null;
        }
    }

    private Bitmap cropBitmapWithBG(Bitmap original, Bitmap bg) {
        final int w = original.getWidth();
        final int h = original.getHeight();

        Bitmap cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cropped);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Paint ptBlur = new Paint();
        ptBlur.setMaskFilter(new BlurMaskFilter(50, BlurMaskFilter.Blur.OUTER));
        int[] offsetXY = new int[2];
        Bitmap bmAlpha = original.extractAlpha(ptBlur, offsetXY);

        Paint ptAlphaColor = new Paint();
        ptAlphaColor.setColor(0xFFFFFFFF);

        canvas.drawBitmap(bg, 0, 0, null);
        canvas.drawBitmap(bmAlpha, offsetXY[0], offsetXY[1], ptAlphaColor);
        canvas.drawBitmap(original, 0, 0, paint);

        bmAlpha.recycle();

        paint.setXfermode(null);
        return cropped;
    }


    private Bitmap cropBitmapWithMask(Bitmap original, Bitmap mask) {
        if (original == null || mask == null) {
            return null;
        }

        final int w = original.getWidth();
        final int h = original.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        Bitmap cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(cropped);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        canvas.drawBitmap(original, 0, 0, null);
        canvas.drawBitmap(mask, 0, 0, paint);


        paint.setXfermode(null);

        return cropped;
    }

    private void openShareMenu() {
        pushAnalyticsLog("Click_On_Share_Button");
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("image/jpeg")
                .setStream(uri)
                .setText("#csu #informationsecurity #kb #mathcsu #chelyabinsk")
                .setChooserTitle("Поделиться с помощью приложения:")
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivity(shareIntent);
    }

    private void pushAnalyticsLog(String channel) {
        Bundle params = new Bundle();
        params.putString( "Click", FieldValue.serverTimestamp().toString());
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance( PreviewImageActivity.this );
        analytics.logEvent( channel, params );
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
            pushAnalyticsLog("Click_On_Save_Button");
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
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
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

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(linearLayoutManager);

        int recyclerViewHeight = recyclerView.getHeight();

        BackgroundListAdapter backgroundListAdapter = new BackgroundListAdapter(this, backgroundsList, recyclerViewHeight);

        backgroundListAdapter.setOnItemClickListener(new BackgroundListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String drawableUri) {
                Log.i(TAG, "Click on drawable: " + drawableUri);

                if (drawableUri.contains("default_pic")) {
                    bg = null;
                } else {
                    bg = BitmapFactory.decodeFile(drawableUri);
                }
                new ThreadSegmentation().execute(":D");
            }
        });

        recyclerView.setAdapter(backgroundListAdapter);
    }
}

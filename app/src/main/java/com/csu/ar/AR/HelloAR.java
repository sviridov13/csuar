//================================================================================================================================
//
//  Copyright (c) 2015-2018 VisionStar Information Technology (Shanghai) Co., Ltd. All Rights Reserved.
//  EasyAR is the registered trademark or trademark of VisionStar Information Technology (Shanghai) Co., Ltd in China
//  and other countries for the augmented reality technology developed by VisionStar Information Technology (Shanghai) Co., Ltd.
//
//================================================================================================================================

package com.csu.ar.AR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import cn.easyar.CameraCalibration;
import cn.easyar.CameraDevice;
import cn.easyar.CameraDeviceFocusMode;
import cn.easyar.CameraDeviceType;
import cn.easyar.CameraFrameStreamer;
import cn.easyar.Frame;
import cn.easyar.ImageTarget;
import cn.easyar.ImageTracker;
import cn.easyar.Renderer;
import cn.easyar.Target;
import cn.easyar.TargetInstance;
import cn.easyar.TargetStatus;
import cn.easyar.Vec2I;
import cn.easyar.Vec4I;
import com.csu.ar.AR.ARVideo;
import com.csu.ar.AR.ArUtils;
import com.csu.ar.AR.ImageRenderer;
import com.csu.ar.AR.VideoRenderer;

public class HelloAR
{
    private final String TAG = this.getClass().getSimpleName();

    private ArUtils arUtils = new ArUtils();

    private CameraDevice camera;
    private CameraFrameStreamer streamer;
    // Список таргетов 2d моделей
    private ArrayList<ImageTracker> imagesTrackers;
    private ArrayList<String> imagesModels;
    // Список таргетов моделей видеофайлов
    private ArrayList<ImageTracker> videoFilesTrackers;
    private ArrayList<String> videoFilesModels;
    // Список таргетов моделей видеопотоков
    private ArrayList<ImageTracker> videoStreamsTrackers;
    private ArrayList<String> videoStreamsModels;

    private int wk_video = 1;
    private int hk_video = 1;
    private int wk_image = 1;
    private int hk_image = 1;

    private Renderer videobg_renderer;
    private ImageRenderer imageRenderer;
    private ArrayList<VideoRenderer> video_renderers;
    private VideoRenderer current_video_renderer;
    private int tracked_target = 0;
    private int active_target = 0;
    private ARVideo video = null;
    private boolean viewport_changed = false;
    private Vec2I view_size = new Vec2I(0, 0);
    private int rotation = 0;
    private Vec4I viewport = new Vec4I(0, 0, 2160, 1080);

    private int cameraDevice = CameraDeviceType.Back;

    public HelloAR() {
        imagesTrackers = new ArrayList<>();
        imagesModels = new ArrayList<>();
        videoFilesTrackers = new ArrayList<>();
        videoFilesModels = new ArrayList<>();
        videoStreamsTrackers = new ArrayList<>();
        videoStreamsModels = new ArrayList<>();
    }

    public boolean initialize(Context context) {
        camera = new CameraDevice();
        streamer = new CameraFrameStreamer();
        streamer.attachCamera(camera);

        boolean status = true;
        status &= camera.open(cameraDevice);
        camera.setSize(new Vec2I(2160, 1080));

        if (!status) { return status; }

        File targetsDir = new File(System.getProperty("java.io.tmpdir") + "/targets");
        File modelsDir = new File(System.getProperty("java.io.tmpdir") + "/models");
        if (targetsDir.exists() && modelsDir.exists()) {
            File[] targetsFiles = targetsDir.listFiles();
            for (File targetsFile : targetsFiles) {
                String targetsFileName = targetsFile.getName();
                //if (targetsFileName.contains(".json")) {

                    ImageTracker imageTracker = new ImageTracker();
                    imageTracker.attachStreamer(streamer);
                    imageTracker.setSimultaneousNum(10);
                    arUtils.loadFromImage(imageTracker, targetsDir + "/" + targetsFileName);
                    String prefix = targetsFileName.substring(0, targetsFileName.indexOf('.'));

                    File[] modelsFiles = modelsDir.listFiles();
                    for (File modelsFile : modelsFiles) {
                        String modelsFileName = modelsFile.getName();
                        if (modelsFileName.contains(prefix))
                        {
                            if  (prefix.contains("image")) {
                                imagesTrackers.add(imageTracker);
                                imagesModels.add(modelsDir + "/" + modelsFileName);
                                break;
                            }

                            if  (prefix.contains("video_stream")) {
                                videoStreamsTrackers.add(imageTracker);
                                videoStreamsModels.add(modelsDir + "/" + modelsFileName);
                                break;
                            }

                            if  (prefix.contains("video_file")) {
                                videoFilesTrackers.add(imageTracker);
                                videoFilesModels.add(modelsDir + "/" + modelsFileName);
                                break;
                            }
                        }
                    }
               // }
            }
        }
        return status;
    }

    public void dispose() {

        if (video != null) {
            video.dispose();
            video = null;
        }
        tracked_target = 0;
        active_target = 0;

        for (ImageTracker tracker : imagesTrackers) {
            tracker.dispose();
        }
        for (ImageTracker tracker : videoFilesTrackers) {
            tracker.dispose();
        }
        for (ImageTracker tracker : videoStreamsTrackers) {
            tracker.dispose();
        }
        imagesTrackers.clear();
        videoFilesTrackers.clear();
        videoStreamsTrackers.clear();

        video_renderers.clear();
        current_video_renderer = null;
        imageRenderer = null;
        if (videobg_renderer != null) {
            videobg_renderer.dispose();
            videobg_renderer = null;
        }
        if (streamer != null) {
            streamer.dispose();
            streamer = null;
        }
        if (camera != null) {
            camera.dispose();
            camera = null;
        }
    }

    public void changeCamera() {
        if (cameraDevice == CameraDeviceType.Back)
            cameraDevice = CameraDeviceType.Front;
        else
            cameraDevice = CameraDeviceType.Back;
        if (camera == null)
            return;
        camera.open(cameraDevice);
        camera.start();
    }

    public boolean start() {
        boolean status = true;
        status &= (camera != null) && camera.start();
        status &= (streamer != null) && streamer.start();
        camera.setFocusMode(CameraDeviceFocusMode.Continousauto);
        for (ImageTracker tracker : imagesTrackers) {
            status &= tracker.start();
        }
        for (ImageTracker tracker : videoFilesTrackers) {
            status &= tracker.start();
        }
        for (ImageTracker tracker : videoStreamsTrackers) {
            status &= tracker.start();
        }
        return status;
    }

    public boolean stop() {
        boolean status = true;
        for (ImageTracker tracker : imagesTrackers) {
            status &= tracker.stop();
        }
        for (ImageTracker tracker : videoFilesTrackers) {
            status &= tracker.stop();
        }
        for (ImageTracker tracker : videoStreamsTrackers) {
            status &= tracker.stop();
        }
        status &= (streamer != null) && streamer.stop();
        status &= (camera != null) && camera.stop();
        return status;
    }

    public void initGL(Context context) {
        if (active_target != 0) {
            video.onLost();
            video.dispose();
            video  = null;
            tracked_target = 0;
            active_target = 0;
        }
        if (videobg_renderer != null) {
            videobg_renderer.dispose();
        }
        videobg_renderer = new Renderer();
        imageRenderer = new ImageRenderer();
        imageRenderer.init(context);
        //
        videobg_renderer = new Renderer();
        video_renderers = new ArrayList<VideoRenderer>();
        for (int k = 0; k < videoStreamsTrackers.size() + videoFilesTrackers.size(); k += 1) {
            VideoRenderer video_renderer = new VideoRenderer();
            video_renderer.init();
            video_renderers.add(video_renderer);
        }
        current_video_renderer = null;
    }

    public void resizeGL(int width, int height) {
        view_size = new Vec2I(width, height);
        viewport_changed = true;
    }

    private void updateViewport() {
        CameraCalibration calib = camera != null ? camera.cameraCalibration() : null;
        int rotation = calib != null ? calib.rotation() : 0;
        if (rotation != this.rotation) {
            this.rotation = rotation;
            viewport_changed = true;
        }
        if (viewport_changed) {
            Vec2I size = new Vec2I(1, 1);
            if ((camera != null) && camera.isOpened()) {
                size = camera.size();
            }
            if (rotation == 90 || rotation == 270) {
                size = new Vec2I(size.data[1], size.data[0]);
            }
            float scaleRatio = Math.max((float) view_size.data[0] / (float) size.data[0], (float) view_size.data[1] / (float) size.data[1]);
            Vec2I viewport_size = new Vec2I(Math.round(size.data[0] * scaleRatio), Math.round(size.data[1] * scaleRatio));
            viewport = new Vec4I((view_size.data[0] - viewport_size.data[0]) / 2, (view_size.data[1] - viewport_size.data[1]) / 2, viewport_size.data[0], viewport_size.data[1]);

            if ((camera != null) && camera.isOpened())
                viewport_changed = false;
        }
    }

    public void render(Context context) {
        GLES20.glClearColor(1.f, 1.f, 1.f, 1.f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (videobg_renderer != null) {
            Vec4I default_viewport = new Vec4I(0, 0, view_size.data[0], view_size.data[1]);
            GLES20.glViewport(default_viewport.data[0], default_viewport.data[1], default_viewport.data[2], default_viewport.data[3]);
            if (videobg_renderer.renderErrorMessage(default_viewport)) {
                return;
            }
        }

        if (streamer == null) { return; }
        Frame frame = streamer.peek();
        try {
            updateViewport();
            GLES20.glViewport(viewport.data[0], viewport.data[1], viewport.data[2], viewport.data[3]);

            if (videobg_renderer != null) {
                videobg_renderer.render(frame, viewport);
            }

            ArrayList<TargetInstance> targetInstances = frame.targetInstances();

            for (TargetInstance targetInstance : frame.targetInstances()) {
                int status = targetInstance.status();
                if (status == TargetStatus.Tracked) {
//                    Log.i(TAG, targetInstance.target().name());
                    Target target = targetInstance.target();
                    int id = target.runtimeID();
                    if (active_target != 0 && active_target != id) {
                        video.onLost();
                        video.dispose();
                        video  = null;
                        tracked_target = 0;
                        active_target = 0;
                    }
                    if (tracked_target == 0) {
                        if (video == null && video_renderers.size() > 0) {
                            int video_rendererID = 0;
                            // загрузка моделей - видеофайлов
                            for (int i = 0; i < videoFilesTrackers.size(); i++) {
                                for (Target testTarget: videoFilesTrackers.get(i).targets()) {
                                    if (testTarget.name().contains(targetInstance.target().name()) && video_renderers.get(video_rendererID).texId() != 0) {
                                        video = new ARVideo();
                                        video.openVideoFile(videoFilesModels.get(i), video_renderers.get(video_rendererID).texId());
                                        current_video_renderer = video_renderers.get(video_rendererID);
                                    }
                                }
                                video_rendererID += 1;
                            }
                            // загрузка моделей - видеопотоков
                            for (int i = 0; i < videoStreamsTrackers.size(); i++) {
                                for (Target testTarget: videoStreamsTrackers.get(i).targets()) {
                                    if (testTarget.name().contains(targetInstance.target().name()) && video_renderers.get(video_rendererID).texId() != 0) {
                                        video = new ARVideo();
                                        // получаю url из .txt файла
                                        String url = "";
                                        File upfile = new File(videoStreamsModels.get(i));
                                        try {
                                            final BufferedReader reader = new BufferedReader(new FileReader(upfile));
                                            url = reader.readLine();
                                            reader.close();
                                        }
                                        catch (final Exception e) { e.printStackTrace(); }
                                        video.openStreamingVideo(url, video_renderers.get(video_rendererID).texId());
                                        current_video_renderer = video_renderers.get(video_rendererID);
                                    }
                                }
                                video_rendererID += 1;
                            }
                        }
                        if (video != null) {
                            video.onFound();
                            tracked_target = id;
                            active_target = id;
                        }
                    }
                    ImageTarget imagetarget = target instanceof ImageTarget ? (ImageTarget)(target) : null;
                    if (imagetarget != null) {
                        if (current_video_renderer != null && video != null) {
                            video.update();
                            if (video.isRenderTextureAvailable()) {
                                current_video_renderer.render(camera.projectionGL(0.2f, 500.f), targetInstance.poseGL(), imagetarget.size());
                            }
                        }
                    }
                    if (imagetarget == null) {
                        continue;
                    }
                    if (imageRenderer != null) {
                        for (int i = 0; i < imagesTrackers.size(); i++) {
                            for (Target testTarget: imagesTrackers.get(i).targets()) {
                                if (testTarget.name().contains(targetInstance.target().name()))
                                    imageRenderer.render(imagesModels.get(i), camera.projectionGL(0.2f, 500.f), targetInstance.poseGL(), imagetarget.size(), wk_image, hk_image);
                            }
                        }
                    }
                }
            }
            if (targetInstances.isEmpty()) {
                if (tracked_target != 0) {
                    video.onLost();
                    tracked_target = 0;
                }
            }
        }
        finally {
            frame.dispose();
        }
    }
}

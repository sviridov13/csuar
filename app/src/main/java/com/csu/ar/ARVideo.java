package com.csu.ar;

import android.util.Log;
import cn.easyar.*;

public class ARVideo
{
    private VideoPlayer player;
    private boolean prepared;
    private boolean found;
    private String path;

    public ARVideo()
    {
        player = new VideoPlayer();
        prepared = false;
        found = false;
    }
    public void dispose()
    {
        player.close();
    }

    public void openVideoFile(String path, int texid)
    {
        this.path = path;
        player.setRenderTexture(texid);
        player.setVideoType(VideoType.Normal);
        player.open(path, StorageType.Absolute, new FunctorOfVoidFromVideoStatus() {
            @Override
            public void invoke(int status) {
                setVideoStatus(status);
            }
        });
    }
    public void openTransparentVideoFile(String path, int texid)
    {
        this.path = path;
        player.setRenderTexture(texid);
        player.setVideoType(VideoType.TransparentSideBySide);
        player.open(path, StorageType.Absolute, new FunctorOfVoidFromVideoStatus() {
            @Override
            public void invoke(int status) {
                setVideoStatus(status);
            }
        });
    }
    public void openStreamingVideo(String url, int texid)
    {
        this.path = url;
        player.setRenderTexture(texid);
        player.setVideoType(VideoType.Normal);
        player.open(url, StorageType.Absolute, new FunctorOfVoidFromVideoStatus() {
            @Override
            public void invoke(int status) {
                setVideoStatus(status);
            }
        });
    }

    public void setVideoStatus(int status)
    {
        Log.i("HelloAR", "video: " + path + " (" + Integer.toString(status) + ")");
        if (status == VideoStatus.Ready) {
            prepared = true;
            if (found) {
                player.play();
            }
        } else if (status == VideoStatus.Completed) {
            if (found) {
                player.play();
            }
        }
    }

    public void onFound()
    {
        found = true;
        if (prepared) {
            player.play();
        }
    }
    public void onLost()
    {
        found = false;
        if (prepared) {
            player.pause();
        }
    }
    public boolean isRenderTextureAvailable()
    {
        return player.isRenderTextureAvailable();
    }
    public void update()
    {
        player.updateFrame();
    }
}

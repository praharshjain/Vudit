package com.praharsh.vudit;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayer extends Activity {
    VideoView vw;
    Uri video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the status bar
        if (Build.VERSION.SDK_INT < 16)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(R.layout.video_player);
        vw = (VideoView) findViewById(R.id.videoView);
        vw.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                if (Build.VERSION.SDK_INT < 16)
                    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                else
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        });
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vw);
        mc.setMediaPlayer(vw);
        try {
            video = Uri.parse(getIntent().getStringExtra("file"));
        } catch (Exception e) {
            finish();
        }
        vw.setMediaController(mc);
        vw.setVideoURI(video);
        vw.requestFocus();
        vw.start();
    }

    @Override
    protected void onStop() {
        if (vw != null) vw.stopPlayback();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (vw != null) vw.stopPlayback();
        super.onDestroy();
    }
}

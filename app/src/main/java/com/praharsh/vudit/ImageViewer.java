package com.praharsh.vudit;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator;
import com.github.piasy.biv.view.BigImageView;
import com.github.piasy.biv.view.GlideImageViewFactory;

import java.io.File;

public class ImageViewer extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar();
        setContentView(R.layout.image_viewer);
        String file_path = getIntent().getStringExtra("file");
        File file = new File(file_path);
        BigImageView preview = findViewById(R.id.preview);
        preview.setProgressIndicator(new ProgressPieIndicator());
        preview.setImageViewFactory(new GlideImageViewFactory());
        preview.showImage(Uri.fromFile(file));
    }

    protected void hideStatusBar() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}

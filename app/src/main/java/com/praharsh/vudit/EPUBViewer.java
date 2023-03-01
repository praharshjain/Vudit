package com.praharsh.vudit;

import static android.net.Uri.encode;

import android.os.Bundle;
import android.view.KeyEvent;

public class EPUBViewer extends WebViewBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //TODO: add icon for epub
        super.onCreate(savedInstanceState);
        String file_path = getIntent().getStringExtra("file");
        try {
            file_path = encode("file://" + file_path, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        wv.loadUrl("file:///android_asset/epubviewer/index.html?file=" + file_path);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}
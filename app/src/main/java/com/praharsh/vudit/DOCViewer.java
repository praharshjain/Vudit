package com.praharsh.vudit;

import android.os.Bundle;

import java.net.URLEncoder;

public class DOCViewer extends WebViewBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String file_path = getIntent().getStringExtra("file");
        boolean isPDF = getIntent().getBooleanExtra("isPDF", false);
        try {
            file_path = URLEncoder.encode("file://" + file_path, "UTF-8");
        } catch (Exception e) {
        }
        if (isPDF)
            wv.loadUrl("file:///android_asset/pdfviewer/web/viewer.html?file=" + file_path);
        else
            wv.loadUrl("file:///android_asset/docviewer/index.html?file=" + file_path);
    }
}
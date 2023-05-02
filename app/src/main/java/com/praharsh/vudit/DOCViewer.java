package com.praharsh.vudit;

import static android.net.Uri.encode;

import android.os.Bundle;

public class DOCViewer extends WebViewBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String file_path = getIntent().getStringExtra("file");
        String type = getIntent().getStringExtra("type");
        try {
            file_path = encode("file://" + file_path, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        switch (type) {
            case "pdf":
                wv.loadUrl("file:///android_asset/pdfviewer/web/viewer.html?file=" + file_path);
                break;
            case "djv":
            case "djvu":
                wv.loadUrl("file:///android_asset/djvuviewer/index.html?file=" + file_path);
                break;
            case "tex":
            case "latex":
                wv.loadUrl("file:///android_asset/latexviewer/index.html?file=" + file_path);
                break;
            default:
                wv.loadUrl("file:///android_asset/docviewer/index.html?file=" + file_path);
        }
    }
}
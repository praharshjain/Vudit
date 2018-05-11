package com.praharsh.vudit;

import android.os.Bundle;
import android.widget.Toast;

import static android.net.Uri.encode;

public class DOCViewer extends WebViewBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String file_path = getIntent().getStringExtra("file");
        boolean isPDF = getIntent().getBooleanExtra("isPDF", false);
        try {
            file_path = encode("file://" + file_path, "UTF-8");
        } catch (Exception e) {
        }
        Toast.makeText(getApplicationContext(), file_path, Toast.LENGTH_LONG).show();
        if (isPDF)
            wv.loadUrl("file:///android_asset/pdfviewer/web/viewer.html?file=" + file_path);
        else
            wv.loadUrl("file:///android_asset/docviewer/index.html?file=" + file_path);
    }
}
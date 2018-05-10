package com.praharsh.vudit;

import android.os.Bundle;

public class HTMLViewer extends WebViewBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String file_path = getIntent().getStringExtra("file");
        wv.loadUrl("file://" + file_path);
    }
}
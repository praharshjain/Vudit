package com.praharsh.vudit;

import android.os.Bundle;
import android.widget.Toast;

import static android.net.Uri.encode;

public class SQLiteViewer extends WebViewBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String file_path = getIntent().getStringExtra("file");
        try {
            file_path = encode("file://" + file_path, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        wv.loadUrl("file:///android_asset/sqliteviewer/index.html?file=" + file_path);
    }
}
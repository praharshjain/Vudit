package com.praharsh.vudit;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static android.net.Uri.encode;

public class TextViewer extends WebViewBaseActivity {
    private String file_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String file_path = getIntent().getStringExtra("file");
        File file = new File(Uri.parse(file_path).getPath());
        try {
            file_path = encode("file://" + file_path, "UTF-8");
        } catch (Exception e) {
        }
        wv.loadUrl("file:///android_asset/textviewer/index.html?file=" + file_path);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            file_text = text.toString();
            wv.addJavascriptInterface(new WebAppInterface(this, file_text), "Android");
            wv.loadUrl("javascript:showFile('" + file_text + "')");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class WebAppInterface {
        private Context mContext;
        private String text;

        WebAppInterface(Context c, String file_text) {
            mContext = c;
            text = file_text;
        }

        @JavascriptInterface
        public String getText() {
            return text;
        }
    }
}
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
            case "wmf":
            case "rtf":
                wv.loadUrl("file:///android_asset/rtfviewer/index.html?file=" + file_path);
                break;
            case "djv":
            case "djvu":
                wv.loadUrl("file:///android_asset/djvuviewer/index.html?file=" + file_path);
                break;
            case "tex":
            case "latex":
                wv.loadUrl("file:///android_asset/latexviewer/index.html?file=" + file_path);
                break;
            case "ipynb":
                wv.loadUrl("file:///android_asset/ipynbviewer/index.html?file=" + file_path);
                break;
            case "pgn":
                wv.loadUrl("file:///android_asset/pgnviewer/index.html?file=" + file_path);
                break;
            case "md":
            case "mkd":
            case "mkdn":
            case "mdwn":
            case "mdown":
            case "markdown":
                wv.loadUrl("file:///android_asset/markdownviewer/index.html?file=" + file_path);
                break;
            default:
                wv.loadUrl("file:///android_asset/docviewer/index.html?file=" + file_path);
        }
    }
}
package com.praharsh.vudit;

import android.os.StatFs;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Util {
    //supported extensions
    static final List<String> audio_ext = Arrays.asList("mp3", "ogg", "wav", "mid", "m4a", "amr");
    static final List<String> image_ext = Arrays.asList("png", "jpg", "gif", "bmp", "jpeg", "webp");
    static final List<String> video_ext = Arrays.asList("mp4", "3gp", "mkv", "webm", "flv", "m4v", "3g2", "avi", "mov", "vob");
    static final List<String> web_ext = Arrays.asList("htm", "html", "js", "xml");
    static final List<String> opendoc_ext = Arrays.asList("odt", "ott", "odp", "otp", "ods", "ots", "fodt", "fods", "fodp");
    static final List<String> txt_ext = Arrays.asList("ascii", "asm", "awk", "bash", "bat", "bf", "bsh", "c", "cert", "cgi", "clj", "conf", "cpp", "cs", "css", "csv", "elr", "go", "h", "hs", "htaccess", "htm", "html", "ini", "java", "js", "json", "key", "lisp", "log", "lua", "md", "mkdn", "pem", "php", "pl", "py", "rb", "readme", "scala", "sh", "sql", "srt", "sub", "tex", "txt", "vb", "vbs", "vhdl", "wollok", "xml", "xsd", "xsl", "yaml", "iml", "gitignore", "gradle");
    //only icons
    static final List<String> archive_ext = Arrays.asList("zip", "jar", "rar", "tar", "gz", "lz", "7z", "tgz", "tlz", "war", "ace", "cab", "dmg", "tar.gz");
    static final List<String> doc_ext = Arrays.asList("doc", "docm", "docx", "dot", "dotm", "dotx", "odt", "ott", "fodt", "rtf", "wps");
    static final List<String> xl_ext = Arrays.asList("xls", "xlsb", "xlsm", "xlt", "xlsx", "xltm", "xltx", "xlw", "ods", "ots", "fods");
    static final List<String> ppt_ext = Arrays.asList("ppt", "pptx", "pptm", "pps", "ppsx", "ppsm", "pot", "potx", "potm", "odp", "otp", "fodp");


    static String[] getMimeTypeQueryArgs(List<String> extArr) {
        int n = extArr.size();
        String mimeType;
        ArrayList<String> selectionArgsList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extArr.get(i));
            if (mimeType != null) {
                selectionArgsList.add(mimeType);
            }
        }
        String[] selectionArgs = new String[selectionArgsList.size()];
        return selectionArgsList.toArray(selectionArgs);
    }

    static String getMimeTypeQuery(String[] selectionArgs) {
        int i, n = selectionArgs.length;
        StringBuilder selectionQuery = new StringBuilder(MediaStore.Files.FileColumns.MIME_TYPE + "=? ");
        for (i = 1; i < n; i++) {
            selectionQuery.append("OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=? ");
        }
        return selectionQuery.toString();
    }

    static long getAvailableMemoryInBytes(String filePath) {
        StatFs stat = new StatFs(filePath);
        return stat.getBlockSize() * (long) stat.getAvailableBlocks();
    }

    static long getTotalMemoryInBytes(String filePath) {
        StatFs stat = new StatFs(filePath);
        return stat.getBlockSize() * (long) stat.getBlockCount();
    }

    static String extension(String name) {
        int i = name.lastIndexOf(".");
        if (i > 0) return name.substring(i + 1).toLowerCase();
        else return "";
    }

    static String displaySize(long bytes) {
        if (bytes > 1073741824) return String.format("%.02f", (float) bytes / 1073741824) + " GB";
        else if (bytes > 1048576) return String.format("%.02f", (float) bytes / 1048576) + " MB";
        else if (bytes > 1024) return String.format("%.02f", (float) bytes / 1024) + " KB";
        else return bytes + " B";
    }

    static String getFormattedTimeDuration(long duration) {
        return String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
    }

    static String getSDCard() {
        String sdcard = System.getenv("SECONDARY_STORAGE");
        if ((sdcard == null) || (sdcard.length() == 0)) {
            sdcard = System.getenv("EXTERNAL_SDCARD_STORAGE");
        }
        return sdcard;
    }

    static String md5(String file_path) {
        try {
            FileInputStream fs = new FileInputStream(file_path);
            MessageDigest md = MessageDigest.getInstance("MD5");
            int bufferSize = 8192, bytes = 0;
            byte[] buffer = new byte[bufferSize];
            do {
                bytes = fs.read(buffer, 0, bufferSize);
                if (bytes > 0)
                    md.update(buffer, 0, bytes);
            } while (bytes > 0);
            byte[] Md5Sum = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Md5Sum.length; i++) {
                String hex = Integer.toHexString(Md5Sum[i] & 0xFF);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}

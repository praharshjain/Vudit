package com.praharsh.vudit;

import static android.os.Build.VERSION.SDK_INT;
import static com.praharsh.vudit.Util.getSDCard;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.gridlayout.widget.GridLayout;

import com.bumptech.glide.Glide;
import com.github.piasy.biv.BigImageViewer;
import com.github.piasy.biv.loader.glide.GlideImageLoader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileViewer extends AppCompatActivity
        implements ListView.OnScrollListener, NavigationView.OnNavigationItemSelectedListener,
        SearchView.OnQueryTextListener {
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1, SDCARD_WRITE_PERMISSION_REQUEST_CODE = 100;
    private static final String tempPath = Environment.getExternalStorageDirectory().getPath() + "/Vudit/temp/";
    private static final String[] requiredPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
    };

    private enum MediaFileType {
        Image, Audio, Video, Document, Archive, Text, APK,
    }

    private static boolean isValid, mBusy = false, recentsView = false, favouritesView = false, homeView = true, sortDesc = false, listFoldersFirst = true, storeRecentItems = true, showHiddenFiles = true;
    private static ViewHolder holder;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ListView lv;
    private GridLayout homeViewLayout;
    private File file;
    private File[] files, origFiles;
    private RecentFilesStack recent;
    private ArrayList<File> favourites;
    private EfficientAdapter adap;
    private FileFilter fileFilter;
    private Intent in;
    private TextView current_duration, total_duration, title, emptyListView;
    private ImageButton btn_play, btn_rev, btn_forward;
    private SeekBar seek;
    private byte[] data;
    private int sortCriterion = 0;
    //Comparators for sorting
    private final Comparator<File> byName = (f1, f2) -> {
        int res = String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
        return (res == 0 ? f1.getName().compareTo(f2.getName()) : res);
    };
    private final Comparator<File> byDate = (f1, f2) -> {
        return Long.compare(f1.lastModified(), f2.lastModified());
    };
    private final Comparator<File> byDateDesc = (f1, f2) -> {
        return Long.compare(f2.lastModified(), f1.lastModified());
    };
    private final Comparator<File> bySize = (f1, f2) -> {
        return Long.compare(f1.length(), f2.length());
    };
    private final Comparator<File> bySizeDesc = (f1, f2) -> {
        return Long.compare(f2.length(), f1.length());
    };

    private static boolean deleteFiles(File f) {
        if (f == null || !f.exists())
            return false;
        f.setWritable(true);
        if (f.isDirectory()) {
            File[] arr = f.listFiles();
            int i, n = arr.length;
            boolean deleted = true;
            for (i = 0; i < n; i++) {
                deleted &= deleteFiles(arr[i]);
            }
            return deleted && f.delete();
        }
        return f.delete();
    }

    private static String unpackZip(File zipFile, File targetDirectory) {
        String dirName = "";
        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            ZipEntry ze;
            int count = -2;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                if (count == -2) {
                    dirName = ze.getName().split("/")[0];
                }
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    return "";
                }
                if (ze.isDirectory()) {
                    continue;
                }
                FileOutputStream fout = new FileOutputStream(file);
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }
                // restore time as well
                long time = ze.getTime();
                if (time > 0) {
                    file.setLastModified(time);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return dirName;
    }

    private static void freeMemory(boolean deleteTempFiles) {
        //remove temp files
        if (deleteTempFiles) {
            deleteFiles(new File(tempPath));
        }
        //try to free ram
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
    }

    private static String getFilePermissions(File file) {
        String s = "";
        if (file.getParent() != null) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("ls", "-l").directory(new File(file.getParent()));
                Process process = processBuilder.start();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                out.flush();
                s = in.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (s == null) s = "";
        return s;
    }

    private static long getFolderSize(File file) {
        long size = 0;
        if (file != null && file.exists() && file.isDirectory()) {
            File[] arr = file.listFiles();
            if (arr != null) {
                for (File child : arr) {
                    if (child.isDirectory())
                        size += getFolderSize(child);
                    else size += child.length();
                }
            }
        }
        return size;
    }

    private static File getRootDirectory() {
        File f = Environment.getRootDirectory();
        f.setReadable(true);
        if (!f.canRead()) {
            return null;
        }
        while (true) {
            File parent = f.getParentFile();
            if (parent == null || !parent.exists()) {
                break;
            }
            parent.setReadable(true);
            if (!parent.canRead()) {
                break;
            }
            f = parent;
        }
        return f;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        BigImageViewer.initialize(GlideImageLoader.with(getApplicationContext()));
        //Setup UI
        setContentView(R.layout.file_viewer);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show());
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        lv = findViewById(R.id.list);
        registerForContextMenu(lv);
        emptyListView = findViewById(R.id.empty);
        lv.setEmptyView(emptyListView);
        lv.setOnItemClickListener((adapterView, view, i, l) -> openFile(files[i]));
        homeViewLayout = findViewById(R.id.home_view);
        homeViewLayout.findViewById(R.id.btn_image_files).setOnClickListener(view -> listMediaFiles(MediaFileType.Image));
        homeViewLayout.findViewById(R.id.btn_music_files).setOnClickListener(view -> listMediaFiles(MediaFileType.Audio));
        homeViewLayout.findViewById(R.id.btn_video_files).setOnClickListener(view -> listMediaFiles(MediaFileType.Video));
        homeViewLayout.findViewById(R.id.btn_document_files).setOnClickListener(view -> listMediaFiles(MediaFileType.Document));
        homeViewLayout.findViewById(R.id.btn_archive_files).setOnClickListener(view -> listMediaFiles(MediaFileType.Archive));
        homeViewLayout.findViewById(R.id.btn_text_files).setOnClickListener(view -> listMediaFiles(MediaFileType.Text));
        homeViewLayout.findViewById(R.id.btn_apps).setOnClickListener(view -> listMediaFiles(MediaFileType.APK));
        homeViewLayout.findViewById(R.id.btn_camera_folder).setOnClickListener(view -> {
            File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            if (f == null || "".equals(f.getPath()) || !f.exists())
                f = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM");
            if (f.exists())
                updateFiles(f);
            else
                showMsg("Camera folder not accessible", 1);
        });
        homeViewLayout.findViewById(R.id.btn_downloads_folder).setOnClickListener(view -> {
            File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (f == null || "".equals(f.getPath()) || !f.exists())
                f = new File(Environment.getExternalStorageDirectory().getPath() + "/Downloads");
            if (f.exists())
                updateFiles(f);
            else
                showMsg("Downloads folder not accessible", 1);
        });
        homeViewLayout.findViewById(R.id.btn_favourites_view).setOnClickListener(view -> favouriteFiles());
        homeViewLayout.findViewById(R.id.btn_recents_view).setOnClickListener(view -> recentFiles());
        //Restore data
        recent = new RecentFilesStack(10);
        favourites = new ArrayList<>();
        restoreData();
        ArrayList<String> storagePaths = new ArrayList<>();
        storagePaths.add(Environment.getExternalStorageDirectory().getPath());
        adap = new EfficientAdapter(getApplicationContext());
        updateFiles(Environment.getExternalStorageDirectory());
        lv.setAdapter(adap);
        lv.setOnScrollListener(this);
        findViewById(R.id.btn_recents_view).setVisibility(storeRecentItems ? View.VISIBLE : View.GONE);
        navigationView.getMenu().findItem(R.id.nav_recent).setVisible(storeRecentItems);
        //Check if secondary storage is available
        String sdcard = getSDCard();
        if (sdcard != null && sdcard.length() > 0) {
            navigationView.getMenu().findItem(R.id.nav_sdcard).setVisible(true);
            storagePaths.add(sdcard);
            requestSDCardPermissions(sdcard);
        }
        //Check if root folder is readable
        File root = getRootDirectory();
        if (root != null && root.canRead()) {
            navigationView.getMenu().findItem(R.id.nav_root).setVisible(true);
        }

        for (int i = 0; i < storagePaths.size(); i++) {
            String path = storagePaths.get(i);
            final File f = new File(path);
            if (f.exists()) {
                RelativeLayout storageView = (RelativeLayout) getLayoutInflater().inflate(R.layout.storage_view, null, false);
                storageView.setBackgroundColor(android.R.drawable.dialog_holo_dark_frame);
                TextView details = storageView.findViewById(R.id.storage_details);
                TextView name = storageView.findViewById(R.id.storage_name);
                ProgressBar storageBar = storageView.findViewById(R.id.storage_bar);
                Long totalMemory = Util.getTotalMemoryInBytes(path), freeMemory = Util.getAvailableMemoryInBytes(path);
                float usedMemory = totalMemory - freeMemory;
                int percent = (int) ((usedMemory / totalMemory) * 100);
                storageBar.setProgress(percent);
                String memoryDetails = Util.displaySize(freeMemory) + " free out of " + Util.displaySize(totalMemory);
                details.setText(memoryDetails);
                if (path.equals(Environment.getExternalStorageDirectory().getPath())) {
                    name.setText("Internal Storage");
                } else {
                    name.setText("External Storage");
                }
                storageView.setOnClickListener(view -> updateFiles(f));
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.columnSpec = GridLayout.spec(0, 2);
                homeViewLayout.addView(storageView, i, params);
            }
        }

        in = getIntent();
        if (Intent.ACTION_VIEW.equals(in.getAction())) {
            Uri uri = in.getData();
            if (uri != null) {
                openFile(getFileFromURI(uri));
            }
        } else {
            switchToHomeView();
        }
    }

    @Override
    protected void onStop() {
        saveData();
        freeMemory(true);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        onStop();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(() -> false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                if (recentsView)
                    recentFiles();
                else if (favouritesView)
                    favouriteFiles();
                else if (homeView)
                    switchToHomeView();
                else if (file != null)
                    updateFiles(file);
                break;
            case R.id.action_info:
                if (file != null)
                    showProperties(file);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        adap.getFilter().filter(query);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            switchToHomeView();
        } else if (id == R.id.nav_root) {
            File f = getRootDirectory();
            updateFiles(f);
        } else if (id == R.id.nav_sdcard) {
            String sdcard = getSDCard();
            if (sdcard != null) {
                File f = new File(sdcard);
                if (f.exists())
                    updateFiles(f);
                else
                    showMsg("External Storage not accessible", 1);
            } else
                showMsg("External Storage not accessible", 1);
        } else if (id == R.id.nav_camera) {
            File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            if (f == null || "".equals(f.getPath()) || !f.exists())
                f = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM");
            if (f.exists())
                updateFiles(f);
            else
                showMsg("Camera folder not accessible", 1);
        } else if (id == R.id.nav_downloads) {
            File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (f == null || "".equals(f.getPath()) || !f.exists())
                f = new File(Environment.getExternalStorageDirectory().getPath() + "/Downloads");
            if (f.exists())
                updateFiles(f);
            else
                showMsg("Downloads folder not accessible", 1);
        } else if (id == R.id.nav_favourites) {
            favouriteFiles();
        } else if (id == R.id.nav_recent) {
            recentFiles();
        } else if (id == R.id.nav_settings) {
            View settings_view = getLayoutInflater().inflate(R.layout.settings_view, null);
            AlertDialog.Builder settings_dialog = new AlertDialog.Builder(FileViewer.this);
            settings_dialog.setIcon(android.R.drawable.ic_menu_preferences);
            settings_dialog.setTitle("Settings");
            settings_dialog.setView(settings_view);
            final CheckBox folders_first_checkbox = settings_view.findViewById(R.id.folders_first_checkbox);
            final CheckBox hidden_files_checkbox = settings_view.findViewById(R.id.hidden_files_checkbox);
            final CheckBox recent_items_checkbox = settings_view.findViewById(R.id.recent_items_checkbox);
            final Spinner sort_criteria = settings_view.findViewById(R.id.sort_criteria);
            final Spinner sort_mode = settings_view.findViewById(R.id.sort_mode);
            folders_first_checkbox.setChecked(listFoldersFirst);
            hidden_files_checkbox.setChecked(showHiddenFiles);
            recent_items_checkbox.setChecked(storeRecentItems);
            sort_criteria.setSelection(sortCriterion);
            sort_mode.setSelection(sortDesc ? 1 : 0);
            settings_view.findViewById(R.id.btn_clear_fav).setOnClickListener(v -> favourites.clear());
            settings_view.findViewById(R.id.btn_clear_recent).setOnClickListener(v -> recent.clear());
            settings_dialog.setPositiveButton("Save", (dialogInterface, i) -> {
                listFoldersFirst = folders_first_checkbox.isChecked();
                showHiddenFiles = hidden_files_checkbox.isChecked();
                storeRecentItems = recent_items_checkbox.isChecked();
                String sortBy = sort_criteria.getSelectedItem().toString();
                sortDesc = sort_mode.getSelectedItem().toString().equals("Descending");
                String[] criteria = getResources().getStringArray(R.array.sort_criteria);
                for (i = 0; i < criteria.length; i++) {
                    if (sortBy.equals(criteria[i])) {
                        sortCriterion = i;
                        break;
                    }
                }
                NavigationView nav = findViewById(R.id.nav_view);
                nav.getMenu().findItem(R.id.nav_recent).setVisible(storeRecentItems);
                if (recentsView) {
                    if (storeRecentItems)
                        recentFiles();
                    else {
                        recentsView = false;
                        updateFiles(Environment.getExternalStorageDirectory());
                    }
                } else if (favouritesView)
                    favouriteFiles();
                else
                    updateFiles(file);
                showMsg("Settings saved", 1);
                dialogInterface.dismiss();
                dialogInterface.cancel();
            });
            settings_dialog.setNegativeButton("Cancel", (dialogInterface, i) -> {
                dialogInterface.dismiss();
                dialogInterface.cancel();
            });
            settings_dialog.show();
        } else if (id == R.id.nav_about) {
            AlertDialog.Builder about_dialog = new AlertDialog.Builder(FileViewer.this);
            about_dialog.setIcon(R.mipmap.ic_launcher);
            about_dialog.setTitle("Vudit");
            about_dialog.setMessage("Version 1.0\nBy - Praharsh Jain\npraharshsamria@gmail.com");
            about_dialog.setPositiveButton("OK", (dialogInterface, i) -> {
                dialogInterface.dismiss();
                dialogInterface.cancel();
            });
            about_dialog.show();
        } else if (id == R.id.nav_feedback) {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:"));
            String[] emailadd = {"praharshsamria@gmail.com"};
            email.putExtra(Intent.EXTRA_EMAIL, emailadd);
            email.putExtra(Intent.EXTRA_SUBJECT, "Vudit Feedback");
            if (email.resolveActivity(getPackageManager()) != null) {
                startActivity(email);
            }
        }
        drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (file != null && file.getParentFile() != null && updateFiles(file.getParentFile())) {
            return;
        } else if (!homeView) {
            switchToHomeView();
        } else super.onBackPressed();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.list) {
            menu.setHeaderIcon(android.R.drawable.ic_menu_manage);
            menu.setHeaderTitle("Actions");
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            int position = info.position;
            File current_file = files[position];
            if (current_file.isDirectory()) {
                menu.add(Menu.NONE, 1, Menu.NONE, "Open");
            } else {
                menu.add(Menu.NONE, 1, Menu.NONE, "Open with default system action");
                String ext = Util.extension(current_file.getName());
                if (Util.web_ext.contains(ext)) {
                    menu.add(Menu.NONE, 2, Menu.NONE, "Preview");
                }
                menu.add(Menu.NONE, 3, Menu.NONE, "Share");
            }
            if (recentsView) {
                menu.add(Menu.NONE, 4, Menu.NONE, "Remove from Recent Items");
            } else if (favouritesView) {
                menu.add(Menu.NONE, 4, Menu.NONE, "Open parent directory");
            } else if (current_file.canWrite()) {
                menu.add(Menu.NONE, 4, Menu.NONE, "Delete");
            }
            menu.add(Menu.NONE, 5, Menu.NONE, favouritesView ? "Remove from Favourites" : "Add to Favourites");
            menu.add(Menu.NONE, 6, Menu.NONE, "Properties");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = menuInfo.position;
        final File current_file = files[position];
        switch (item.getItemId()) {
            case 1:
                if (current_file.isDirectory()) {
                    updateFiles(current_file);
                } else {
                    MimeTypeMap myMime = MimeTypeMap.getSingleton();
                    Intent in = new Intent(Intent.ACTION_VIEW);
                    String mimeType = myMime.getMimeTypeFromExtension(Util.extension(current_file.getName()));
                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), current_file);
                    in.setDataAndType(uri, mimeType);
                    in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(in);
                    } catch (ActivityNotFoundException e) {
                        showMsg("No handler for this type of file.", 1);
                    }
                }
                return true;
            case 2:
                Intent in = new Intent(FileViewer.this, HTMLViewer.class);
                in.putExtra("file", current_file.getPath());
                startActivity(in);
                return true;
            case 3:
                Intent share = new Intent();
                share.setAction(Intent.ACTION_SEND);
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                String mimeType = myMime.getMimeTypeFromExtension(Util.extension(current_file.getName()));
                Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), current_file);
                share.setType(mimeType);
                share.putExtra(Intent.EXTRA_STREAM, uri);
                try {
                    startActivity(share);
                } catch (Exception e) {
                    e.printStackTrace();
                    showMsg("No handler available to share", 1);
                }
                return true;
            case 4:
                if (favouritesView) {
                    File f = current_file.getParentFile();
                    if (f != null && f.exists()) {
                        updateFiles(f);
                    } else {
                        showMsg("Parent directory can not be opened", 1);
                    }
                } else if (recentsView) {
                    int i = recent.indexOf(current_file);
                    if (i >= 0) {
                        recent.remove(i);
                        recentFiles();
                        showMsg(current_file.getName() + " removed from Recent Items", 1);
                    }
                } else {
                    AlertDialog.Builder confirmation_dialog = new AlertDialog.Builder(FileViewer.this);
                    confirmation_dialog.setIcon(android.R.drawable.ic_delete);
                    confirmation_dialog.setTitle("Delete");
                    confirmation_dialog.setMessage("Are you sure you want to delete " + current_file.getName() + "?");
                    confirmation_dialog.setPositiveButton("Yes", (dialogInterface, btn) -> {
                        if (deleteFiles(current_file)) {
                            showMsg(current_file.getName() + " successfully deleted", 1);
                            updateFiles(current_file.getParentFile());
                            //update recents and favorites
                            int i, n = favourites.size();
                            File[] arr = new File[n];
                            favourites.toArray(arr);
                            favourites.clear();
                            for (i = 0; i < n; i++) {
                                File f = arr[i];
                                if (f.exists())
                                    favourites.add(f);
                            }
                            RecentFilesStack temp = (RecentFilesStack<File>) recent.clone();
                            n = temp.size();
                            recent.clear();
                            for (i = 0; i < n; i++) {
                                File f = (File) temp.get(i);
                                if (f.exists())
                                    recent.push(f);
                            }
                        } else {
                            showMsg(current_file.getName() + " could not be deleted", 1);
                        }
                        dialogInterface.dismiss();
                        dialogInterface.cancel();
                    });
                    confirmation_dialog.setNegativeButton("No", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        dialogInterface.cancel();
                    });
                    confirmation_dialog.show();
                }
                return true;
            case 5:
                if (favouritesView) {
                    int i = favourites.indexOf(current_file);
                    if (i >= 0) {
                        favourites.remove(i);
                        favouriteFiles();
                        showMsg(current_file.getName() + " removed from Favourites", 1);
                    }
                } else {
                    if (favourites.size() < 20) {
                        favourites.add(current_file);
                        showMsg(current_file.getName() + " added to Favourites", 1);
                    } else
                        showMsg("Favourites list is full", 1);
                }
                return true;
            case 6:
                showProperties(current_file);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i1, int i2) {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == SDCARD_WRITE_PERMISSION_REQUEST_CODE && SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Uri treeUri = resultData.getData();
            if (treeUri != null) {
                grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }
    }

    private void openFile(File current_file) {
        if (current_file == null || !current_file.exists())
            return;
        current_file.setReadable(true);
        if (!current_file.canRead()) {
            showMsg((current_file.isDirectory() ? "Folder" : "File") + " is not readable", 1);
            return;
        }
        recent.push(current_file);
        freeMemory(false);
        final MediaPlayer mp = new MediaPlayer();
        final MediaMetadataRetriever meta = new MediaMetadataRetriever();
        if (current_file.isDirectory()) {
            updateFiles(current_file);
        } else if (current_file.isFile()) {
            isValid = true;
            String ext = Util.extension(current_file.getName());
            if ("pdf".equals(ext)) {
                in = new Intent(FileViewer.this, DOCViewer.class);
                in.putExtra("file", current_file.getPath());
                in.putExtra("isPDF", true);
                startActivity(in);
            } else if ("sqlite".equals(ext)) {
                in = new Intent(FileViewer.this, SQLiteViewer.class);
                in.putExtra("file", current_file.getPath());
                startActivity(in);
            } else if ("zip".equals(ext)) {
                String dirName = unpackZip(current_file, new File(tempPath));
                if (dirName.length() > 0) {
                    File f = new File(tempPath + dirName);
                    updateFiles(f);
                } else {
                    showMsg("Zip file is not valid", 1);
                    freeMemory(true);
                }
            } else if ("svg".equals(ext)) {
                Intent in = new Intent(FileViewer.this, HTMLViewer.class);
                in.putExtra("file", current_file.getPath());
                startActivity(in);
            } else if ("epub".equals(ext)) {
                in = new Intent(FileViewer.this, EPUBViewer.class);
                in.putExtra("file", current_file.getPath());
                startActivity(in);
            } else if (Util.opendoc_ext.contains(ext)) {
                in = new Intent(FileViewer.this, DOCViewer.class);
                in.putExtra("file", current_file.getPath());
                in.putExtra("isPDF", false);
                startActivity(in);
            } else {
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                Intent in = new Intent(Intent.ACTION_VIEW);
                String mimeType = myMime.getMimeTypeFromExtension(Util.extension(current_file.getName()));
                if ((mimeType != null && mimeType.contains("text")) || Util.txt_ext.contains(ext)) {
                    in = new Intent(FileViewer.this, TextViewer.class);
                    in.putExtra("file", current_file.getPath());
                    startActivity(in);
                } else if ((mimeType != null && mimeType.contains("image")) || Util.image_ext.contains(ext)) {
                    in = new Intent(FileViewer.this, ImageViewer.class);
                    in.putExtra("file", current_file.getPath());
                    startActivity(in);
                } else if ((mimeType != null && mimeType.contains("video")) || Util.video_ext.contains(ext)) {
                    in = new Intent(FileViewer.this, VideoPlayer.class);
                    in.putExtra("file", current_file.getPath());
                    startActivity(in);
                } else if ((mimeType != null && mimeType.contains("audio")) || Util.audio_ext.contains(ext)) {
                    try {
                        mp.setDataSource(current_file.getPath());
                        mp.prepare();
                        meta.setDataSource(current_file.getPath());
                        data = meta.getEmbeddedPicture();
                    } catch (Exception e) {
                        isValid = false;
                        e.printStackTrace();
                    }
                    if (isValid) {
                        ImageView album_art, icon;
                        final View player = getLayoutInflater().inflate(R.layout.music_player, null);
                        btn_play = player.findViewById(R.id.btn_play);
                        btn_rev = player.findViewById(R.id.btn_rev);
                        btn_forward = player.findViewById(R.id.btn_forward);
                        current_duration = player.findViewById(R.id.current_duration);
                        total_duration = player.findViewById(R.id.total_duration);
                        seek = player.findViewById(R.id.seek);
                        title = player.findViewById(R.id.title);
                        icon = player.findViewById(R.id.imageView);
                        album_art = player.findViewById(R.id.album_art);
                        title.setText(current_file.getName());
                        icon.setImageResource(R.drawable.file_music);
                        if (data != null) {
                            album_art.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
                        }
                        int duration = mp.getDuration();
                        seek.setMax(duration);
                        total_duration.setText(Util.getFormattedTimeDuration(duration));
                        final Handler handler = new Handler();
                        //Make sure you update Seekbar on UI thread
                        final Runnable updateseek = new Runnable() {
                            @Override
                            public void run() {
                                int pos = mp.getCurrentPosition();
                                seek.setProgress(pos);
                                current_duration.setText(Util.getFormattedTimeDuration(pos));
                                handler.postDelayed(this, 1000);
                            }
                        };
                        FileViewer.this.runOnUiThread(updateseek);
                        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
                                if (fromUser) {
                                    mp.seekTo(position);
                                    current_duration.setText(Util.getFormattedTimeDuration(position));
                                }
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        });
                        btn_play.setOnClickListener(view -> {
                            if (mp.isPlaying()) {
                                mp.pause();
                                btn_play.setImageResource(android.R.drawable.ic_media_play);
                            } else {
                                mp.start();
                                btn_play.setImageResource(android.R.drawable.ic_media_pause);
                            }
                        });
                        btn_forward.setOnClickListener(view -> {
                            int max = mp.getDuration();
                            int newpos = mp.getCurrentPosition() + 5000;
                            if (newpos > max) {
                                mp.seekTo(max);
                                seek.setProgress(max);
                            } else {
                                mp.seekTo(newpos);
                                seek.setProgress(newpos);
                            }
                        });
                        btn_rev.setOnClickListener(view -> {
                            int newpos = mp.getCurrentPosition() - 5000;
                            if (newpos > 0) {
                                mp.seekTo(newpos);
                                seek.setProgress(newpos);
                            } else {
                                mp.seekTo(0);
                                seek.setProgress(0);
                            }
                        });
                        final AlertDialog.Builder player_dialog = new AlertDialog.Builder(new ContextThemeWrapper(FileViewer.this, android.R.style.Theme_Black));
                        player_dialog.setOnCancelListener(dialogInterface -> {
                            handler.removeCallbacks(updateseek);
                            mp.stop();
                            mp.reset();
                        });
                        player_dialog.setView(player);
                        player_dialog.show();
                    } else {
                        showMsg("Invalid music file", 1);
                    }
                } else {
                    try {
                        Uri uri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName(), current_file);
                        in.setDataAndType(uri, mimeType);
                        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(in);
                    } catch (ActivityNotFoundException e) {
                        showMsg("No handler for this type of file.", 1);
                    } catch (Exception e) {
                        showMsg("Cannot open this file.", 1);
                    }
                }
            }
        }
    }

    private void showProperties(final File current_file) {
        MediaPlayer mp = new MediaPlayer();
        MediaMetadataRetriever meta = new MediaMetadataRetriever();
        String info = "", bitrate = "";
        int duration;
        AlertDialog.Builder properties_dialog = new AlertDialog.Builder(FileViewer.this);
        View properties_view = getLayoutInflater().inflate(R.layout.properties_view, null);
        final TextView name = properties_view.findViewById(R.id.name);
        final TextView type = properties_view.findViewById(R.id.type);
        final TextView time = properties_view.findViewById(R.id.time);
        final TextView size = properties_view.findViewById(R.id.size);
        final TextView location = properties_view.findViewById(R.id.location);
        final TextView details = properties_view.findViewById(R.id.details);
        //Fetch properties
        name.setText(current_file.getName());
        location.setText(current_file.getPath());
        SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
        time.setText(format.format(current_file.lastModified()));
        properties_dialog.setTitle("Properties");
        properties_dialog.setView(properties_view);
        String file_info = getFilePermissions(current_file);
        if (file_info.length() > 29) {
            info += "Permissions : " + file_info.substring(1, 10);
            info += "\nOwner : " + file_info.substring(10, 19);
            info += "\nGroup : " + file_info.substring(19, 29);
        }
        info += "\nReadable : " + (current_file.canRead() ? "YES" : "NO");
        info += "\nHidden : " + (current_file.isHidden() ? "YES" : "NO");
        if (current_file.isFile()) {
            isValid = true;
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            String mimeType = myMime.getMimeTypeFromExtension(Util.extension(current_file.getName()));
            mimeType = ((mimeType == null || mimeType.equals("")) ? "Unknown" : mimeType);
            type.setText(mimeType);
            size.setText(Util.displaySize(current_file.length()));
            String ext = Util.extension(current_file.getName());
            if ("apk".equals(ext)) {
                String path = current_file.getPath();
                PackageManager pm = getPackageManager();
                PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
                pi.applicationInfo.sourceDir = path;
                pi.applicationInfo.publicSourceDir = path;
                properties_dialog.setIcon(R.drawable.file_apk);
                info += "\nPackage Name : " + pi.packageName;
                info += "\nVersion Name : " + pi.versionName;
                info += "\nVersion Code : " + pi.versionCode;
                info += "\nApp Installed : " + (appInstalled(pi.packageName) ? "Yes" : "No");
            } else if (Util.audio_ext.contains(ext)) {
                properties_dialog.setIcon(R.drawable.file_music);
                try {
                    mp.setDataSource(current_file.getPath());
                    mp.prepare();
                    meta.setDataSource(current_file.getPath());
                } catch (Exception e) {
                    isValid = false;
                    e.printStackTrace();
                }
                if (isValid) {
                    duration = mp.getDuration();
                    mp.reset();
                    mp.release();
                    String album = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    album = ((album == null || "".equals(album)) ? "Unknown" : album);
                    String artist = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    artist = ((artist == null || "".equals(artist)) ? "Unknown" : artist);
                    String genre = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                    genre = ((genre == null || "".equals(genre)) ? "Unknown" : genre);
                    String year = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
                    year = ((year == null || "".equals(year)) ? "Unknown" : year);
                    bitrate = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                    bitrate = ((bitrate == null || "".equals(bitrate)) ? "Unknown" : bitrate);
                    try {
                        meta.release();
                    } catch (IOException e) {
                    }
                    info += "\nTrack Duration : " + Util.getFormattedTimeDuration(duration);
                    info += "\nAlbum : " + album;
                    info += "\nArtist : " + artist;
                    info += "\nGenre : " + genre;
                    info += "\nYear : " + year;
                    info += "\nBitrate : " + bitrate;
                } else {
                    info += "\nInvalid File";
                }
            } else if (Util.image_ext.contains(ext)) {
                properties_dialog.setIcon(new BitmapDrawable(getResources(), ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(current_file.getPath()), 50, 50)));
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                //Returns null, sizes are in the options variable
                BitmapFactory.decodeFile(current_file.getPath(), options);
                info += "\nWidth : " + options.outWidth + " pixels";
                info += "\nHeight : " + options.outHeight + " pixels";
            } else if (Util.video_ext.contains(ext)) {
                properties_dialog.setIcon(new BitmapDrawable(getResources(), ThumbnailUtils.createVideoThumbnail(current_file.getPath(), MediaStore.Images.Thumbnails.MINI_KIND)));
                try {
                    mp.setDataSource(current_file.getPath());
                    mp.prepare();
                    meta.setDataSource(current_file.getPath());
                } catch (Exception e) {
                    isValid = false;
                    e.printStackTrace();
                }
                if (isValid) {
                    duration = mp.getDuration();
                    mp.reset();
                    mp.release();
                    bitrate = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                    bitrate = ((bitrate == null || "".equals(bitrate)) ? "Unknown" : bitrate);
                    String frame_rate = "";
                    if (SDK_INT >= Build.VERSION_CODES.M) {
                        frame_rate = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
                    }
                    frame_rate = ((frame_rate == null || "".equals(frame_rate)) ? "Unknown" : frame_rate);
                    String height = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    height = ((height == null || "".equals(height)) ? "Unknown" : height);
                    String width = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    width = ((width == null || "".equals(width)) ? "Unknown" : width);
                    try {
                        meta.release();
                    } catch (IOException e) {
                    }
                    info += "\nTrack Duration : " + Util.getFormattedTimeDuration(duration);
                    info += "\nBitrate : " + bitrate;
                    info += "\nWidth : " + width;
                    info += "\nHeight : " + height;
                    info += "\nFrame Rate : " + frame_rate;
                } else {
                    info += "\nInvalid File";
                }
            } else {
                properties_dialog.setIcon(R.drawable.file_default);
            }
            details.setText(info + "\nMD5 Checksum : calculating...");
            final Handler h = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    String s = details.getText().toString();
                    s = s.substring(0, s.length() - 14);
                    details.setText(s + data.getString("MD5"));
                }
            };
            Thread t = new Thread() {
                @Override
                public void run() {
                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("MD5", Util.md5(current_file.getPath()));
                    msg.setData(data);
                    msg.setTarget(h);
                    h.sendMessage(msg);
                }
            };
            t.start();
        } else if (current_file.isDirectory()) {
            type.setText("Directory");
            size.setText("calculating...");
            if (current_file.listFiles() != null) {
                int n = current_file.listFiles().length;
                properties_dialog.setIcon(n > 0 ? R.drawable.folder : R.drawable.folder_empty);
                info += "\n" + (n > 0 ? "Contains " + n + " Items" : "Empty Folder");
            }
            details.setText(info);
            final Handler h = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Bundle data = msg.getData();
                    size.setText(data.getString("folder_size"));
                }
            };
            Thread t = new Thread() {
                @Override
                public void run() {
                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("folder_size", Util.displaySize(getFolderSize(current_file)));
                    msg.setData(data);
                    msg.setTarget(h);
                    h.sendMessage(msg);
                }
            };
            t.start();
        }
        properties_dialog.setPositiveButton("OK", (dialogInterface, i) -> {
            dialogInterface.dismiss();
            dialogInterface.cancel();
        });
        properties_dialog.show();
    }

    private boolean updateFiles(File f) {
        if (f != null && f.exists() && f.listFiles() != null) {
            f.setReadable(true);
            f.setWritable(true);
            file = f;
            files = f.listFiles();
            files = sortFiles(files);
            origFiles = files.clone();
            toolbar.setTitle(file.getName());
            recentsView = false;
            favouritesView = false;
            updateList();
            return true;
        }
        return false;
    }

    private void recentFiles() {
        RecentFilesStack temp = (RecentFilesStack<File>) recent.clone();
        files = new File[temp.size()];
        for (int i = 0; temp.size() > 0; i++) {
            files[i] = (File) temp.pop();
        }
        origFiles = files.clone();
        toolbar.setTitle("Recent Items");
        recentsView = true;
        favouritesView = false;
        updateList();
    }

    private void favouriteFiles() {
        files = new File[favourites.size()];
        files = favourites.toArray(files);
        files = sortFiles(files);
        origFiles = files.clone();
        toolbar.setTitle("Favourites");
        favouritesView = true;
        recentsView = false;
        updateList();
    }

    private void updateList() {
        homeView = false;
        lv.setVisibility(View.VISIBLE);
        emptyListView.setVisibility(View.VISIBLE);
        homeViewLayout.setVisibility(View.GONE);
        if (adap != null) {
            adap.notifyDataSetChanged();
            mBusy = false;
        }
        lv.smoothScrollToPosition(0);
    }

    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        switch (scrollState) {
            case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                mBusy = false;
                refreshList(view);
                break;
            default:
                mBusy = true;
        }
    }

    private void refreshList(final AbsListView view) {
        int first = view.getFirstVisiblePosition();
        int count = view.getChildCount();
        for (int i = 0; i < count; i++) {
            final File current_file = files[first + i];
            if (!current_file.exists())
                continue;
            holder.icon = view.getChildAt(i).findViewById(R.id.icon);
            if (current_file.isDirectory()) {
                holder.details.setText("");
                File[] temp = current_file.listFiles();
                int n = (temp != null ? temp.length : 0);
                holder.details.setText(n + " items");
                holder.icon.setImageResource(n > 0 ? R.drawable.folder : R.drawable.folder_empty);
            } else {
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                String mimeType = myMime.getMimeTypeFromExtension(Util.extension(current_file.getName()));
                holder.details.setText(Util.displaySize(current_file.length()));
                String ext = Util.extension(current_file.getName());
                if ("apk".equals(ext)) {
                    String path = current_file.getPath();
                    PackageManager pm = getPackageManager();
                    PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
                    pi.applicationInfo.sourceDir = path;
                    pi.applicationInfo.publicSourceDir = path;
                    holder.icon.setImageDrawable(pi.applicationInfo.loadIcon(pm));
                } else if ("pdf".equals(ext)) {
                    holder.icon.setImageResource(R.drawable.file_pdf);
                } else if ("epub".equals(ext)) {
                    holder.icon.setImageResource(R.drawable.file_epub);
                } else if ("svg".equals(ext)) {
                    Glide.with(getApplicationContext()).load(Uri.fromFile(current_file)).placeholder(R.drawable.file_svg).into(holder.icon);
                } else if ("csv".equals(ext)) {
                    holder.icon.setImageResource(R.drawable.file_csv);
                } else if ("sqlite".equals(ext)) {
                    holder.icon.setImageResource(R.drawable.file_sqlite);
                } else if ((mimeType != null && mimeType.contains("audio")) || Util.audio_ext.contains(ext)) {
                    Glide.with(getApplicationContext()).load(Uri.fromFile(current_file)).placeholder(R.drawable.file_music).into(holder.icon);
                } else if ((mimeType != null && mimeType.contains("image")) || Util.image_ext.contains(ext)) {
                    Glide.with(getApplicationContext()).load(Uri.fromFile(current_file)).placeholder(R.drawable.file_image).into(holder.icon);
                } else if ((mimeType != null && mimeType.contains("video")) || Util.video_ext.contains(ext)) {
                    Glide.with(getApplicationContext()).load(Uri.fromFile(current_file)).placeholder(R.drawable.file_video).into(holder.icon);
                } else if (Util.archive_ext.contains(ext)) {
                    holder.icon.setImageResource(R.drawable.file_archive);
                } else if (Util.doc_ext.contains(ext)) {
                    holder.icon.setImageResource(R.drawable.file_doc);
                } else if (Util.xl_ext.contains(ext)) {
                    holder.icon.setImageResource(R.drawable.file_xl);
                } else if (Util.ppt_ext.contains(ext)) {
                    holder.icon.setImageResource(R.drawable.file_ppt);
                } else {
                    holder.icon.setImageResource(R.drawable.file_default);
                }
            }
        }
    }

    //Utility functions
    private void restoreData() {
        SharedPreferences prefs = getSharedPreferences("Vudit_Settings", MODE_PRIVATE);
        listFoldersFirst = prefs.getBoolean("FoldersFirst", true);
        showHiddenFiles = prefs.getBoolean("ShowHidden", true);
        storeRecentItems = prefs.getBoolean("ShowRecents", true);
        sortDesc = prefs.getBoolean("SortDesc", false);
        sortCriterion = prefs.getInt("SortCriterion", 0);
        prefs = getSharedPreferences("Vudit_Recent_Items", MODE_PRIVATE);
        Collection<?> c = prefs.getAll().values();
        String[] paths = new String[c.size()];
        paths = c.toArray(paths);
        File f;
        recent.clear();
        for (int i = 0; i < paths.length; i++) {
            f = new File(paths[i]);
            if (f.exists())
                recent.push(f);
        }
        prefs = getSharedPreferences("Vudit_Favourites", MODE_PRIVATE);
        c = prefs.getAll().values();
        paths = new String[c.size()];
        paths = c.toArray(paths);
        favourites.clear();
        for (int i = 0; i < paths.length; i++) {
            f = new File(paths[i]);
            if (f.exists())
                favourites.add(f);
        }
    }

    private boolean saveData() {
        try {
            SharedPreferences.Editor recent_editor = getSharedPreferences("Vudit_Recent_Items", MODE_PRIVATE).edit();
            SharedPreferences.Editor favourites_editor = getSharedPreferences("Vudit_Favourites", MODE_PRIVATE).edit();
            SharedPreferences.Editor settings_editor = getSharedPreferences("Vudit_Settings", MODE_PRIVATE).edit();
            settings_editor.clear();
            settings_editor.commit();
            settings_editor.putBoolean("FoldersFirst", listFoldersFirst);
            settings_editor.putBoolean("ShowHidden", showHiddenFiles);
            settings_editor.putBoolean("ShowRecents", storeRecentItems);
            settings_editor.putBoolean("SortDesc", sortDesc);
            settings_editor.putInt("SortCriterion", sortCriterion);
            settings_editor.commit();
            recent_editor.clear();
            recent_editor.commit();
            File f;
            RecentFilesStack temp = (RecentFilesStack<File>) recent.clone();
            int i, n = temp.size();
            for (i = 0; i < n; i++) {
                f = (File) temp.pop();
                if (f.exists())
                    recent_editor.putString(f.getName(), f.getPath());
            }
            recent_editor.commit();
            favourites_editor.clear();
            favourites_editor.commit();
            n = favourites.size();
            File[] arr = new File[n];
            favourites.toArray(arr);
            for (i = 0; i < n; i++) {
                f = arr[i];
                if (f.exists())
                    favourites_editor.putString(f.getName(), f.getPath());
            }
            favourites_editor.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private File getFileFromURI(Uri uri) {
        // try file first
        String path = uri.getPath();
        File f = new File(path);
        if (f.exists()) {
            return f;
        }
        String dirPath = Environment.getExternalStorageDirectory().getPath();
        int idx = path.indexOf(dirPath);
        if (idx > 0) {
            //path overlap found
            path = path.substring(idx + dirPath.length());
        }
        f = new File(dirPath + path);
        if (f.exists()) {
            return f;
        }
        // TODO: create temp file? using getContentResolver().openInputStream(uri);
        showMsg("File not found: " + path, 1);
        return null;
    }

    private File[] sortFiles(File[] f) {
        int i, n;
        if (!showHiddenFiles) {
            ArrayList<File> temp = new ArrayList<File>();
            n = f.length;
            for (i = 0; i < n; i++) {
                if (!f[i].isHidden())
                    temp.add(f[i]);
            }
            f = new File[temp.size()];
            f = temp.toArray(f);
        }
        //Sort Alphabetically
        Arrays.sort(f, byName);
        if (sortCriterion == 1)
            Arrays.sort(f, sortDesc ? byDateDesc : byDate);
        else if (sortCriterion == 2)
            Arrays.sort(f, sortDesc ? bySizeDesc : bySize);
        else if (sortDesc) {
            File temp;
            n = f.length;
            for (i = 0; i < n / 2; i++) {
                temp = f[i];
                f[i] = f[n - i - 1];
                f[n - i - 1] = temp;
            }
        }
        if (listFoldersFirst) {
            try {
                Arrays.sort(f, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        return Boolean.compare(!f1.isDirectory(), !f2.isDirectory());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int res : grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                showMsg("Permissions not granted", 0);
                return;
            }
        }
    }

    private List<String> getNeededPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(permission);
                }
            }
        }
        return listPermissionsNeeded;
    }

    private void checkAndRequestPermissions() {
        List<String> neededPermissions = getNeededPermissions();
        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toArray(new String[neededPermissions.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        }
        if (SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Intent in = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            in.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(in);
        }
    }

    private void showMsg(String msg, int mode) {
        if (mode == 0)
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        else
            Snackbar.make(findViewById(R.id.list), msg, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private boolean appInstalled(String uri) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void listMediaFiles(MediaFileType type) {
        String toolbarTitle = "";
        String selectionQuery = null;
        String[] selectionArgs = null;
        ArrayList<File> fileList = null;
        Uri externalURI = MediaStore.Files.getContentUri("external");
        Uri internalURI = MediaStore.Files.getContentUri("internal");
        switch (type) {
            case Image:
                toolbarTitle = "Pictures";
                fileList = getFileListFromQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selectionQuery, selectionArgs);
                fileList.addAll(getFileListFromQuery(MediaStore.Images.Media.INTERNAL_CONTENT_URI, selectionQuery, selectionArgs));
                break;
            case Audio:
                toolbarTitle = "Music";
                fileList = getFileListFromQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, selectionQuery, selectionArgs);
                fileList.addAll(getFileListFromQuery(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, selectionQuery, selectionArgs));
                break;
            case Video:
                toolbarTitle = "Videos";
                fileList = getFileListFromQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, selectionQuery, selectionArgs);
                fileList.addAll(getFileListFromQuery(MediaStore.Video.Media.INTERNAL_CONTENT_URI, selectionQuery, selectionArgs));
                break;
            case Document:
                toolbarTitle = "Documents";
                List<String> extList = new ArrayList<>(Util.doc_ext);
                extList.addAll(Util.xl_ext);
                extList.addAll(Util.ppt_ext);
                extList.addAll(Util.opendoc_ext);
                extList.add("pdf");
                selectionArgs = Util.getMimeTypeQueryArgs(extList);
                selectionQuery = Util.getMimeTypeQuery(selectionArgs);
                fileList = getFileListFromQuery(externalURI, selectionQuery, selectionArgs);
                fileList.addAll(getFileListFromQuery(internalURI, selectionQuery, selectionArgs));
                break;
            case Archive:
                toolbarTitle = "Archives";
                selectionArgs = Util.getMimeTypeQueryArgs(Util.archive_ext);
                selectionQuery = Util.getMimeTypeQuery(selectionArgs);
                fileList = getFileListFromQuery(externalURI, selectionQuery, selectionArgs);
                fileList.addAll(getFileListFromQuery(internalURI, selectionQuery, selectionArgs));
                break;
            case Text:
                toolbarTitle = "Text files";
                selectionArgs = Util.getMimeTypeQueryArgs(Util.txt_ext);
                selectionQuery = Util.getMimeTypeQuery(selectionArgs);
                fileList = getFileListFromQuery(externalURI, selectionQuery, selectionArgs);
                fileList.addAll(getFileListFromQuery(internalURI, selectionQuery, selectionArgs));
                break;
            case APK:
                toolbarTitle = "Apps";
                selectionArgs = Util.getMimeTypeQueryArgs(Collections.singletonList("apk"));
                selectionQuery = Util.getMimeTypeQuery(selectionArgs);
                fileList = getFileListFromQuery(externalURI, selectionQuery, selectionArgs);
                fileList.addAll(getFileListFromQuery(internalURI, selectionQuery, selectionArgs));
        }
        files = new File[fileList.size()];
        files = fileList.toArray(files);
        if (files != null) {
            file = null;
            files = sortFiles(files);
            origFiles = files.clone();
            toolbar.setTitle(toolbarTitle);
            recentsView = false;
            favouritesView = false;
            updateList();
        } else {
            files = origFiles;
            showMsg("No " + toolbarTitle + " found", 1);
        }
    }

    private ArrayList<File> getFileListFromQuery(Uri uri, String selectionQuery, String[] selectionArgs) {
        Cursor cursor = getContentResolver().query(uri, null, selectionQuery, selectionArgs, null);
        int colIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
        ArrayList<File> fileList = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String data = cursor.getString(colIndex);
                File f = new File(data);
                if (f != null && f.exists()) {
                    fileList.add(f);
                }
            }
        }
        cursor.close();
        return fileList;
    }

    private void requestSDCardPermissions(String cardPath) {
        if (SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SDCARD_WRITE_PERMISSION_REQUEST_CODE);
        }
    }

    private void switchToHomeView() {
        homeView = true;
        recentsView = false;
        favouritesView = false;
        toolbar.setTitle("Vudit");
        homeViewLayout.setVisibility(View.VISIBLE);
        lv.setVisibility(View.GONE);
        emptyListView.setVisibility(View.GONE);
    }

    private static class ViewHolder {
        private TextView name;
        private TextView date;
        private TextView details;
        private ImageView icon;
    }

    class EfficientAdapter extends BaseAdapter implements Filterable {
        private final LayoutInflater mInflater;
        private final Context mContext;

        public EfficientAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContext = context;
        }

        @Override
        public int getCount() {
            return files != null ? files.length : 0;
        }

        @Override
        public Object getItem(int i) {
            return files[i];
        }

        @Override
        public long getItemId(int i) {
            return files[i].hashCode();
        }

        public View getView(int position, View view, ViewGroup parent) {
            // A ViewHolder keeps references to child views to avoid unneccessary calls to findViewById() on each row.
            // When view is not null, reuse it directly, no need to reinflate it.
            if (view == null) {
                // Creates a ViewHolder and store references to the child views we want to bind data to.
                view = mInflater.inflate(R.layout.list_item, parent, false);
                holder = new ViewHolder();
                holder.name = view.findViewById(R.id.name);
                holder.date = view.findViewById(R.id.date);
                holder.details = view.findViewById(R.id.details);
                holder.icon = view.findViewById(R.id.icon);
                view.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the child views
                holder = (ViewHolder) view.getTag();
            }
            File current_file = files[position];
            holder.name.setText(current_file.getName());
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss a");
            holder.date.setText(format.format(current_file.lastModified()));
            if (current_file.isFile()) {
                holder.details.setText(Util.displaySize(current_file.length()));
            } else if (current_file.isDirectory()) {
                File[] temp = current_file.listFiles();
                int n = (temp != null ? temp.length : 0);
                holder.details.setText(n + " items");
            } else {
                holder.details.setText("");
            }
            if (!mBusy) {
                if (current_file.isDirectory()) {
                    holder.icon.setImageResource(R.drawable.folder);
                } else {
                    holder.icon.setImageResource(R.drawable.file_default);
                }
            } else {
                holder.icon.setImageResource(R.drawable.loading);
            }
            return view;
        }

        @Override
        public Filter getFilter() {
            if (fileFilter == null) {
                fileFilter = new FileFilter();
            }
            return fileFilter;
        }
    }

    class RecentFilesStack<File> extends Stack<File> {
        private final int maxSize;

        public RecentFilesStack(int size) {
            super();
            this.maxSize = size;
        }

        @Override
        public File push(File f) {
            int i = this.indexOf(f);
            if (i >= 0)
                this.remove(i);
            while (this.size() >= maxSize) {
                this.remove(0);
            }
            return super.push(f);
        }
    }

    class FileFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            if (constraint != null && constraint.length() > 0) {
                ArrayList<File> tempList = new ArrayList<File>();
                for (File f : origFiles) {
                    if (f.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        tempList.add(f);
                    }
                }
                filterResults.count = tempList.size();
                File[] f = new File[filterResults.count];
                filterResults.values = tempList.toArray(f);
            } else {
                filterResults.count = files.length;
                filterResults.values = files;
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults results) {
            files = (File[]) results.values;
            updateList();
        }
    }
}
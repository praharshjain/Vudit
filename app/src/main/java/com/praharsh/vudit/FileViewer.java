package com.praharsh.vudit;

import android.Manifest;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.os.StatFs;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static android.os.Build.VERSION.SDK_INT;

public class FileViewer extends AppCompatActivity
        implements ListView.OnScrollListener, NavigationView.OnNavigationItemSelectedListener,
        SearchView.OnQueryTextListener {
    private static boolean mBusy = false, recentsView = false, favouritesView = false, homeView = true;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1, SDCARD_WRITE_PERMISSION_REQUEST_CODE = 100;
    private static ViewHolder holder;
    private static DocumentFile pickedDir;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;
    private ListView lv;
    private LinearLayout homeViewLayout;
    private File file, files[], origFiles[];
    private RecentFilesStack recent;
    private ArrayList<File> favourites;
    private EfficientAdapter adap;
    private FileFilter fileFilter;
    private Intent in;
    private TextView current_duration, total_duration, title;
    private ImageView album_art, icon;
    private ImageButton btn_play, btn_rev, btn_forward;
    private SeekBar seek;
    private static final String tempPath = Environment.getExternalStorageDirectory().getPath() + "/Vudit/temp/";
    private byte data[];
    private int sortCriterion = 0;
    private boolean isValid, sortDesc = false, listFoldersFirst = true, storeRecentItems = true, showHiddenFiles = true;
    //Comparators for sorting
    private Comparator<File> byName = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            int res = String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
            return (res == 0 ? f1.getName().compareTo(f2.getName()) : res);
        }
    };
    private Comparator<File> byDate = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            if (f1.lastModified() > f2.lastModified()) return 1;
            else if (f1.lastModified() < f2.lastModified()) return -1;
            else return 0;
        }
    };
    private Comparator<File> byDateDesc = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            if (f1.lastModified() > f2.lastModified()) return -1;
            else if (f1.lastModified() < f2.lastModified()) return 1;
            else return 0;
        }
    };
    private Comparator<File> bySize = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            if (f1.length() > f2.length()) return 1;
            else if (f1.length() < f2.length()) return -1;
            else return 0;
        }
    };
    private Comparator<File> bySizeDesc = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            if (f1.length() > f2.length()) return -1;
            else if (f1.length() < f2.length()) return 1;
            else return 0;
        }
    };
    //supported extensions
    private static final String audio_ext[] = {"mp3", "oog", "wav", "mid", "m4a", "amr"};
    private static final String image_ext[] = {"png", "jpg", "gif", "bmp", "jpeg", "webp"};
    private static final String video_ext[] = {"mp4", "3gp", "mkv", "webm", "flv", "m4v"};
    private static final String web_ext[] = {"htm", "html", "js", "xml"};
    private static final String opendoc_ext[] = {"odt", "ott", "odp", "otp", "ods", "ots", "fodt", "fods", "fodp"};
    private static final String txt_ext[] = {"ascii", "asm", "awk", "bash", "bat", "bf", "bsh", "c", "cert", "cgi", "clj", "conf", "cpp", "cs", "css", "csv", "elr", "go", "h", "hs", "htaccess", "htm", "html", "ini", "java", "js", "json", "key", "lisp", "log", "lua", "md", "mkdn", "pem", "php", "pl", "py", "rb", "readme", "scala", "sh", "sql", "srt", "sub", "tex", "txt", "vb", "vbs", "vhdl", "wollok", "xml", "xsd", "xsl", "yaml", "iml", "gitignore", "gradle"};
    //only icon
    private static final String archive_ext[] = {"zip", "jar", "rar", "tar", "gz", "lz", "7z", "tgz", "tlz", "war", "ace", "cab", "dmg", "tar.gz"};
    private static final String doc_ext[] = {"doc", "docm", "docx", "dot", "dotm", "dotx", "odt", "ott", "fodt", "rtf", "wps"};
    private static final String xl_ext[] = {"xls", "xlsb", "xlsm", "xlt", "xlsx", "xltm", "xltx", "xlw", "ods", "ots", "fods"};
    private static final String ppt_ext[] = {"ppt", "pptx", "pptm", "pps", "ppsx", "ppsm", "pot", "potx", "potm", "odp", "otp", "fodp"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkAndRequestPermissions()) {
            showMsg("Permissions not granted", 0);
            finish();
            return;
        }
        //Setup UI
        setContentView(R.layout.file_viewer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                listMediaFiles(4);
            }
        });
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        lv = (ListView) findViewById(R.id.list);
        registerForContextMenu(lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                openFile(files[i]);
            }
        });
        homeViewLayout = (LinearLayout) findViewById(R.id.home_view);
        homeViewLayout.findViewById(R.id.btn_image_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listMediaFiles(1);
            }
        });
        homeViewLayout.findViewById(R.id.btn_music_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listMediaFiles(2);
            }
        });
        homeViewLayout.findViewById(R.id.btn_video_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listMediaFiles(3);
            }
        });
        homeViewLayout.findViewById(R.id.btn_document_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listMediaFiles(4);
            }
        });
        homeViewLayout.findViewById(R.id.btn_archive_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listMediaFiles(5);
            }
        });
        homeViewLayout.findViewById(R.id.btn_text_files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listMediaFiles(6);
            }
        });
        homeViewLayout.findViewById(R.id.btn_camera_folder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                if (f == null || f.getPath().equals("") || !f.exists())
                    f = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM");
                if (f.exists())
                    updateFiles(f);
                else
                    showMsg("Camera folder not accessible", 1);
            }
        });
        homeViewLayout.findViewById(R.id.btn_downloads_folder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (f == null || f.getPath().equals("") || !f.exists())
                    f = new File(Environment.getExternalStorageDirectory().getPath() + "/Downloads");
                if (f.exists())
                    updateFiles(f);
                else
                    showMsg("Downloads folder not accessible", 1);
            }
        });
        homeViewLayout.findViewById(R.id.btn_favourites_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                favouriteFiles();
            }
        });
        homeViewLayout.findViewById(R.id.btn_recents_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recentFiles();
            }
        });
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
        String sdcard = System.getenv("SECONDARY_STORAGE");
        if ((sdcard == null) || (sdcard.length() == 0)) {
            sdcard = System.getenv("EXTERNAL_SDCARD_STORAGE");
        }
        if (sdcard != null && sdcard.length() > 0) {
            navigationView.getMenu().findItem(R.id.nav_sdcard).setVisible(true);
            storagePaths.add(sdcard);
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SDCARD_WRITE_PERMISSION_REQUEST_CODE);
        }
        for (int i = 0; i < storagePaths.size(); i++) {
            String path = storagePaths.get(i);
            final File f = new File(path);
            if (f != null && f.exists()) {
                RelativeLayout storageView = (RelativeLayout) getLayoutInflater().inflate(R.layout.storage_view, null, false);
                TextView details = (TextView) storageView.findViewById(R.id.storage_details);
                TextView name = (TextView) storageView.findViewById(R.id.storage_name);
                ProgressBar storageBar = (ProgressBar) storageView.findViewById(R.id.storage_bar);
                Long totalMemory = getTotalMemoryInBytes(path), freeMemory = getAvailableMemoryInBytes(path);
                float usedMemory = totalMemory - freeMemory;
                int percent = (int) ((usedMemory / totalMemory) * 100);
                storageBar.setProgress(percent);
                String memoryDetails = displaySize(freeMemory) + " free out of " + displaySize(totalMemory);
                details.setText(memoryDetails);
                if (path.equals(Environment.getExternalStorageDirectory().getPath())) {
                    name.setText("Internal Storage");
                } else {
                    name.setText("External Storage");
                }
                LinearLayout l = (LinearLayout) findViewById(R.id.home_view);
                storageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        updateFiles(f);
                    }
                });
                l.addView(storageView, 0);
            }
        }

        in = getIntent();
        if (Intent.ACTION_VIEW.equals(in.getAction()) && in.getType() != null) {
            openFile(new File(in.getData().getPath()));
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
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return false;
            }
        });
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
                else
                    //Todo: something here
                    ;
                break;
            case R.id.action_info:
                if (file != null)
                    showProperties(file);
                else
                    //Todo: something here
                    ;
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
            File f = Environment.getRootDirectory();
            while (f.getParentFile() != null)
                f = f.getParentFile();
            updateFiles(f);
        } else if (id == R.id.nav_sdcard) {
            String sdcard = System.getenv("SECONDARY_STORAGE");
            if ((sdcard == null) || (sdcard.length() == 0)) {
                sdcard = System.getenv("EXTERNAL_SDCARD_STORAGE");
            }
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
            if (f == null || f.getPath().equals("") || !f.exists())
                f = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM");
            if (f.exists())
                updateFiles(f);
            else
                showMsg("Camera folder not accessible", 1);
        } else if (id == R.id.nav_downloads) {
            File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (f == null || f.getPath().equals("") || !f.exists())
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
            final CheckBox folders_first_checkbox = (CheckBox) settings_view.findViewById(R.id.folders_first_checkbox);
            final CheckBox hidden_files_checkbox = (CheckBox) settings_view.findViewById(R.id.hidden_files_checkbox);
            final CheckBox recent_items_checkbox = (CheckBox) settings_view.findViewById(R.id.recent_items_checkbox);
            final Spinner sort_criteria = (Spinner) settings_view.findViewById(R.id.sort_criteria);
            final Spinner sort_mode = (Spinner) settings_view.findViewById(R.id.sort_mode);
            folders_first_checkbox.setChecked(listFoldersFirst);
            hidden_files_checkbox.setChecked(showHiddenFiles);
            recent_items_checkbox.setChecked(storeRecentItems);
            sort_criteria.setSelection(sortCriterion);
            sort_mode.setSelection(sortDesc ? 1 : 0);
            settings_dialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    listFoldersFirst = folders_first_checkbox.isChecked();
                    showHiddenFiles = hidden_files_checkbox.isChecked();
                    storeRecentItems = recent_items_checkbox.isChecked();
                    String sortBy = sort_criteria.getSelectedItem().toString();
                    sortDesc = sort_mode.getSelectedItem().toString().equals("Descending");
                    String criteria[] = getResources().getStringArray(R.array.sort_criteria);
                    for (i = 0; i < criteria.length; i++) {
                        if (sortBy.equals(criteria[i])) {
                            sortCriterion = i;
                            break;
                        }
                    }
                    NavigationView nav = (NavigationView) findViewById(R.id.nav_view);
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
                }
            });
            settings_dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    dialogInterface.cancel();
                }
            });
            settings_dialog.show();
        } else if (id == R.id.nav_about) {
            AlertDialog.Builder about_dialog = new AlertDialog.Builder(FileViewer.this);
            about_dialog.setIcon(R.mipmap.ic_launcher);
            about_dialog.setTitle("Vudit");
            about_dialog.setMessage("Version 1.0\nBy - Praharsh Jain\npraharshsamria@gmail.com");
            about_dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    dialogInterface.cancel();
                }
            });
            about_dialog.show();
        } else if (id == R.id.nav_feedback) {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:"));
            String emailadd[] = {"praharshsamria@gmail.com"};
            email.putExtra(Intent.EXTRA_EMAIL, emailadd);
            email.putExtra(Intent.EXTRA_SUBJECT, "Vudit Feedback");
            if (email.resolveActivity(getPackageManager()) != null) {
                startActivity(email);
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
                String ext = extension(current_file.getName());
                if (Arrays.asList(web_ext).contains(ext))
                    menu.add(Menu.NONE, 2, Menu.NONE, "Preview");
                menu.add(Menu.NONE, 3, Menu.NONE, "Share");
            }
            if (recentsView) {
                menu.add(Menu.NONE, 4, Menu.NONE, "Remove from Recent Items");
            } else if (favouritesView) {
                menu.add(Menu.NONE, 4, Menu.NONE, "Open parent directory");
            } else {
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
                    String mimeType = myMime.getMimeTypeFromExtension(extension(current_file.getName()));
                    in.setDataAndType(Uri.fromFile(current_file), mimeType);
                    in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
                String mimeType = myMime.getMimeTypeFromExtension(extension(current_file.getName()));
                share.setType(mimeType);
                share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(current_file));
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
                    confirmation_dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int btn) {
                            if (deleteFiles(current_file)) {
                                showMsg(current_file.getName() + " successfully deleted", 1);
                                updateFiles(current_file.getParentFile());
                                //update recents and favorites
                                int i, n = favourites.size();
                                File arr[] = new File[n];
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
                        }
                    });
                    confirmation_dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            dialogInterface.cancel();
                        }
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
        if (resultCode != SDCARD_WRITE_PERMISSION_REQUEST_CODE)
            return;
        else {
            if (SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Uri treeUri = resultData.getData();
                pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }
    }

    private void openFile(File current_file) {
        if (!current_file.exists())
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
            String ext = extension(current_file.getName());
            if (ext.equals("pdf")) {
                in = new Intent(FileViewer.this, DOCViewer.class);
                in.putExtra("file", current_file.getPath());
                in.putExtra("isPDF", true);
                startActivity(in);
            } else if (ext.equals("sqlite")) {
                in = new Intent(FileViewer.this, SQLiteViewer.class);
                in.putExtra("file", current_file.getPath());
                startActivity(in);
            } else if (ext.equals("zip")) {
                String dirName = unpackZip(current_file, new File(tempPath));
                if (dirName.length() > 0) {
                    File f = new File(tempPath + dirName);
                    updateFiles(f);
                } else {
                    showMsg("Zip file is not valid", 1);
                    freeMemory(true);
                }
            } else if (ext.equals("svg")) {
                Intent in = new Intent(FileViewer.this, HTMLViewer.class);
                in.putExtra("file", current_file.getPath());
                startActivity(in);
            } else if (Arrays.asList(audio_ext).contains(ext)) {
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
                    final View player = getLayoutInflater().inflate(R.layout.music_player, null);
                    player.findViewById(R.id.icon);
                    btn_play = (ImageButton) player.findViewById(R.id.btn_play);
                    btn_rev = (ImageButton) player.findViewById(R.id.btn_rev);
                    btn_forward = (ImageButton) player.findViewById(R.id.btn_forward);
                    current_duration = (TextView) player.findViewById(R.id.current_duration);
                    total_duration = (TextView) player.findViewById(R.id.total_duration);
                    seek = (SeekBar) player.findViewById(R.id.seek);
                    title = (TextView) player.findViewById(R.id.title);
                    icon = (ImageView) player.findViewById(R.id.imageView);
                    album_art = (ImageView) player.findViewById(R.id.album_art);
                    title.setText(current_file.getName());
                    icon.setImageResource(R.drawable.file_music);
                    if (data != null) {
                        album_art.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
                    }
                    int duration = mp.getDuration();
                    seek.setMax(duration);
                    total_duration.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))));
                    final Handler handler = new Handler();
                    //Make sure you update Seekbar on UI thread
                    final Runnable updateseek = new Runnable() {
                        @Override
                        public void run() {
                            if (mp != null) {
                                int pos = mp.getCurrentPosition();
                                seek.setProgress(pos);
                                current_duration.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(pos), TimeUnit.MILLISECONDS.toSeconds(pos) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(pos))));
                            }
                            handler.postDelayed(this, 1000);
                        }
                    };
                    FileViewer.this.runOnUiThread(updateseek);
                    seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int position, boolean fromUser) {
                            if (mp != null && fromUser) {
                                mp.seekTo(position);
                                current_duration.setText(String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(position), TimeUnit.MILLISECONDS.toSeconds(position) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(position))));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });
                    btn_play.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mp.isPlaying()) {
                                mp.pause();
                                btn_play.setImageResource(android.R.drawable.ic_media_play);
                            } else {
                                mp.start();
                                btn_play.setImageResource(android.R.drawable.ic_media_pause);
                            }
                        }
                    });
                    btn_forward.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int max = mp.getDuration();
                            int newpos = mp.getCurrentPosition() + 5000;
                            if (newpos > max) {
                                mp.seekTo(max);
                                seek.setProgress(max);
                            } else {
                                mp.seekTo(newpos);
                                seek.setProgress(newpos);
                            }
                        }
                    });
                    btn_rev.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int newpos = mp.getCurrentPosition() - 5000;
                            if (newpos > 0) {
                                mp.seekTo(newpos);
                                seek.setProgress(newpos);
                            } else {
                                mp.seekTo(0);
                                seek.setProgress(0);
                            }
                        }
                    });
                    final AlertDialog.Builder player_dialog = new AlertDialog.Builder(new ContextThemeWrapper(FileViewer.this, android.R.style.Theme_Black));
                    player_dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            handler.removeCallbacks(updateseek);
                            mp.stop();
                            mp.reset();
                        }
                    });
                    player_dialog.setView(player);
                    final AlertDialog ad = player_dialog.show();
                } else {
                    showMsg("Invalid music file", 1);
                }
            } else if (Arrays.asList(image_ext).contains(ext)) {
                AlertDialog.Builder preview_dialog = new AlertDialog.Builder(new ContextThemeWrapper(FileViewer.this, android.R.style.Theme_Black));
                View image_view = getLayoutInflater().inflate(R.layout.image_viewer, null);
                final ImageView preview = (ImageView) image_view.findViewById(R.id.preview);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                //Returns null, sizes are in the options variable
                BitmapFactory.decodeFile(current_file.getPath(), options);
                int height = options.outHeight;
                int width = options.outWidth;
                while (height * width * 4 > 4 * 1024 * 1024) {
                    height /= 2;
                    width /= 2;
                }
                preview.setImageBitmap(decodeSampledBitmap(current_file.getPath(), width, height));
                preview_dialog.setTitle(current_file.getName());
                preview_dialog.setView(image_view);
                preview_dialog.show();
            } else if (Arrays.asList(video_ext).contains(ext)) {
                in = new Intent(FileViewer.this, VideoPlayer.class);
                in.putExtra("file", current_file.getPath());
                startActivity(in);
            } else if (Arrays.asList(opendoc_ext).contains(ext)) {
                in = new Intent(FileViewer.this, DOCViewer.class);
                in.putExtra("file", current_file.getPath());
                in.putExtra("isPDF", false);
                startActivity(in);
            } else {
                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                Intent in = new Intent(Intent.ACTION_VIEW);
                String mimeType = myMime.getMimeTypeFromExtension(extension(current_file.getName()));
                if ((mimeType != null && mimeType.contains("text")) || Arrays.asList(txt_ext).contains(ext)) {
                    in = new Intent(FileViewer.this, TextViewer.class);
                    in.putExtra("file", current_file.getPath());
                    startActivity(in);
                } else {
                    in.setDataAndType(Uri.fromFile(current_file), mimeType);
                    in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(in);
                    } catch (ActivityNotFoundException e) {
                        showMsg("No handler for this type of file.", 1);
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
        TextView name = (TextView) properties_view.findViewById(R.id.name);
        TextView type = (TextView) properties_view.findViewById(R.id.type);
        TextView time = (TextView) properties_view.findViewById(R.id.time);
        final TextView size = (TextView) properties_view.findViewById(R.id.size);
        final TextView details = (TextView) properties_view.findViewById(R.id.details);
        //Fetch properties
        name.setText(current_file.getName());
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
            String mimeType = myMime.getMimeTypeFromExtension(extension(current_file.getName()));
            mimeType = ((mimeType == null || mimeType.equals("")) ? "Unknown" : mimeType);
            type.setText(mimeType);
            size.setText(displaySize(current_file.length()));
            String ext = extension(current_file.getName());
            if (ext.equals("apk")) {
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
            } else if (Arrays.asList(audio_ext).contains(ext)) {
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
                    album = ((album == null || album.equals("")) ? "Unknown" : album);
                    String artist = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    artist = ((artist == null || artist.equals("")) ? "Unknown" : artist);
                    String genre = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                    genre = ((genre == null || genre.equals("")) ? "Unknown" : genre);
                    String year = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
                    year = ((year == null || year.equals("")) ? "Unknown" : year);
                    bitrate = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                    bitrate = ((bitrate == null || bitrate.equals("")) ? "Unknown" : bitrate);
                    meta.release();
                    info += "\nTrack Duration : " + String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
                    info += "\nAlbum : " + album;
                    info += "\nArtist : " + artist;
                    info += "\nGenre : " + genre;
                    info += "\nYear : " + year;
                    info += "\nBitrate : " + bitrate;
                } else {
                    info += "\nInvalid File";
                }
            } else if (Arrays.asList(image_ext).contains(ext)) {
                properties_dialog.setIcon(new BitmapDrawable(getResources(), ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(current_file.getPath()), 50, 50)));
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                //Returns null, sizes are in the options variable
                BitmapFactory.decodeFile(current_file.getPath(), options);
                info += "\nWidth : " + options.outWidth + " pixels";
                info += "\nHeight : " + options.outHeight + " pixels";
            } else if (Arrays.asList(video_ext).contains(ext)) {
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
                    bitrate = ((bitrate == null || bitrate.equals("")) ? "Unknown" : bitrate);
                    String frame_rate = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
                    frame_rate = ((frame_rate == null || frame_rate.equals("")) ? "Unknown" : frame_rate);
                    String height = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    height = ((height == null || height.equals("")) ? "Unknown" : height);
                    String width = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    width = ((width == null || width.equals("")) ? "Unknown" : width);
                    meta.release();
                    info += "\nTrack Duration : " + String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration), TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
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
                    data.putString("MD5", MD5(current_file.getPath()));
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
                    data.putString("folder_size", displaySize(getFolderSize(current_file)));
                    msg.setData(data);
                    msg.setTarget(h);
                    h.sendMessage(msg);
                }
            };
            t.start();
        }
        properties_dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                dialogInterface.cancel();
            }
        });
        properties_dialog.show();
    }

    private boolean updateFiles(File f) {
        if (f.exists() && f.listFiles() != null) {
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

    private boolean deleteFiles(File f) {
        if (f == null || !f.exists())
            return false;
        f.setWritable(true);
        if (f.isDirectory()) {
            File arr[] = f.listFiles();
            int i, n = arr.length;
            boolean deleted = true;
            for (i = 0; i < n; i++) {
                deleted &= deleteFiles(arr[i]);
            }
            return deleted & f.delete();
        } else return f.delete();
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
                int first = view.getFirstVisiblePosition();
                int count = view.getChildCount();
                for (int i = 0; i < count; i++) {
                    final File current_file = files[first + i];
                    if (!current_file.exists())
                        continue;
                    holder.icon = (ImageView) view.getChildAt(i).findViewById(R.id.icon);
                    if (current_file.isDirectory()) {
                        holder.details.setText("");
                        File temp[] = current_file.listFiles();
                        int n = (temp != null ? temp.length : 0);
                        holder.details.setText(n + " items");
                        holder.icon.setImageResource(n > 0 ? R.drawable.folder : R.drawable.folder_empty);
                    } else {
                        holder.details.setText(displaySize(current_file.length()));
                        String ext = extension(current_file.getName());
                        if (ext.equals("apk")) {
                            String path = current_file.getPath();
                            PackageManager pm = getPackageManager();
                            PackageInfo pi = pm.getPackageArchiveInfo(path, 0);
                            pi.applicationInfo.sourceDir = path;
                            pi.applicationInfo.publicSourceDir = path;
                            holder.icon.setImageDrawable(pi.applicationInfo.loadIcon(pm));
                        } else if (ext.equals("pdf")) {
                            holder.icon.setImageResource(R.drawable.file_pdf);
                        } else if (ext.equals("svg")) {
                            holder.icon.setImageResource(R.drawable.file_svg);
                        } else if (ext.equals("csv")) {
                            holder.icon.setImageResource(R.drawable.file_csv);
                        } else if (ext.equals("sqlite")) {
                            holder.icon.setImageResource(R.drawable.file_sqlite);
                        } else if (Arrays.asList(audio_ext).contains(ext)) {
                            holder.icon.setImageResource(R.drawable.file_music);
                        } else if (Arrays.asList(image_ext).contains(ext)) {
                            holder.icon.setImageBitmap(ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(current_file.getPath()), 50, 50));
                        } else if (Arrays.asList(video_ext).contains(ext)) {
                            holder.icon.setImageBitmap(ThumbnailUtils.createVideoThumbnail(current_file.getPath(), MediaStore.Images.Thumbnails.MINI_KIND));
                        } else if (Arrays.asList(archive_ext).contains(ext)) {
                            holder.icon.setImageResource(R.drawable.file_archive);
                        } else if (Arrays.asList(doc_ext).contains(ext)) {
                            holder.icon.setImageResource(R.drawable.file_doc);
                        } else if (Arrays.asList(xl_ext).contains(ext)) {
                            holder.icon.setImageResource(R.drawable.file_xl);
                        } else if (Arrays.asList(ppt_ext).contains(ext)) {
                            holder.icon.setImageResource(R.drawable.file_ppt);
                        } else {
                            holder.icon.setImageResource(R.drawable.file_default);
                        }
                    }
                    holder.icon.setTag(null);
                }
                break;
            default:
                mBusy = true;
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
        String paths[] = new String[c.size()];
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
            File arr[] = new File[n];
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

    private File[] sortFiles(File f[]) {
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
        else {
            if (sortDesc) {
                File temp;
                n = f.length;
                for (i = 0; i < n / 2; i++) {
                    temp = f[i];
                    f[i] = f[n - i - 1];
                    f[n - i - 1] = temp;
                }
            }
        }
        if (listFoldersFirst) {
            try {
                Arrays.sort(f, new Comparator<File>() {
                    @Override
                    public int compare(File f1, File f2) {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        else if (!f1.isDirectory() && f2.isDirectory()) return 1;
                        else return 0;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    private boolean checkAndRequestPermissions() {
        if (SDK_INT >= Build.VERSION_CODES.M) {
            int permissionStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int permissionWriteStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            List<String> listPermissionsNeeded = new ArrayList<>();
            if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
                return false;
            } else {
                return true;
            }
        }
        return true;
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

    private void freeMemory(boolean deleteTempFiles) {
        //remove temp files
        if (deleteTempFiles) {
            deleteFiles(new File(tempPath));
        }
        //try to free ram
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();
    }

    private void showMsg(String msg, int mode) {
        if (mode == 0)
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        else
            Snackbar.make(findViewById(R.id.list), msg, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    private String displaySize(long bytes) {
        if (bytes > 1073741824) return String.format("%.02f", (float) bytes / 1073741824) + " GB";
        else if (bytes > 1048576) return String.format("%.02f", (float) bytes / 1048576) + " MB";
        else if (bytes > 1024) return String.format("%.02f", (float) bytes / 1024) + " KB";
        else return bytes + " B";
    }

    private String extension(String name) {
        int i = name.lastIndexOf(".");
        if (i > 0) return name.substring(i + 1).toLowerCase();
        else return "";
    }

    private String getFilePermissions(File file) {
        String s = "";
        if (file.getParent() != null) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder("ls", "-l").directory(new File(file.getParent()));// TODO CHECK IF THE FILE IS SD CARD PARENT IS NULL
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
        long size;
        if (file.exists() && file.isDirectory()) {
            size = 0;
            File arr[] = file.listFiles();
            if (arr != null) {
                for (File child : arr) {
                    if (child.isDirectory())
                        size += getFolderSize(child);
                    else size += child.length();
                }
            }
            return size;
        } else return 0;
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

    private String MD5(String file_path) {
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

    private static int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static Bitmap decodeSampledBitmap(String pathToFile, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathToFile, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathToFile, options);
    }

    private long getAvailableMemoryInBytes(String filePath) {
        StatFs stat = new StatFs(filePath);
        return stat.getBlockSize() * (long) stat.getAvailableBlocks();
    }

    private long getTotalMemoryInBytes(String filePath) {
        StatFs stat = new StatFs(filePath);
        return stat.getBlockSize() * (long) stat.getBlockCount();
    }

    private void listMediaFiles(int type) {
        Uri uri = null;
        String toolbarTitle = "";
        String selectionQuery = null;
        String[] selectionArgs = null;
        int i, n;
        switch (type) {
            case 1: //images
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                toolbarTitle = "Pictures";
                break;
            case 2: //audio
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                toolbarTitle = "Music";
                break;
            case 3: //video
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                toolbarTitle = "Videos";
                break;
            case 4: //documents
                uri = MediaStore.Files.getContentUri("external");
                toolbarTitle = "Documents";
                ArrayList<String> extList = new ArrayList(Arrays.asList(doc_ext));
                extList.addAll(Arrays.asList(xl_ext));
                extList.addAll(Arrays.asList(ppt_ext));
                String[] extArr = new String[extList.size()];
                extArr = extList.toArray(extArr);
                selectionArgs = getMimeTypeQueryArgs(extArr);
                selectionQuery = getMimeTypeQuery(selectionArgs);
                break;
            case 5: //archives
                uri = MediaStore.Files.getContentUri("external");
                toolbarTitle = "Archives";
                selectionArgs = getMimeTypeQueryArgs(archive_ext);
                selectionQuery = getMimeTypeQuery(selectionArgs);
                break;
            case 6: //text files
                uri = MediaStore.Files.getContentUri("external");
                toolbarTitle = "Text files";
                selectionArgs = getMimeTypeQueryArgs(txt_ext);
                selectionQuery = getMimeTypeQuery(selectionArgs);
                break;
        }
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, selectionQuery, selectionArgs, null);
        ArrayList<File> fileList = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                File f = new File(data);
                if (f != null && f.exists()) {
                    fileList.add(f);
                }
            }
        }
        cursor.close();
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

    private void switchToHomeView() {
        homeView = true;
        toolbar.setTitle("Vudit");
        homeViewLayout.setVisibility(View.VISIBLE);
        lv.setVisibility(View.GONE);
    }

    private String[] getMimeTypeQueryArgs(String extArr[]) {
        int n = extArr.length;
        String mimeType = null;
        ArrayList<String> selectionArgsList = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extArr[i]);
            if (mimeType != null) {
                selectionArgsList.add(mimeType);
            }
        }
        String[] selectionArgs = new String[selectionArgsList.size()];
        selectionArgs = selectionArgsList.toArray(selectionArgs);
        return selectionArgs;
    }

    private String getMimeTypeQuery(String[] selectionArgs) {
        int i, n = selectionArgs.length;
        String selectionQuery = MediaStore.Files.FileColumns.MIME_TYPE + "=? ";
        for (i = 1; i < n; i++) {
            selectionQuery += "OR " + MediaStore.Files.FileColumns.MIME_TYPE + "=? ";
        }
        return selectionQuery;
    }

    static class ViewHolder {
        TextView name;
        TextView date;
        TextView details;
        ImageView icon;
    }

    class EfficientAdapter extends BaseAdapter implements Filterable {
        private LayoutInflater mInflater;
        private Context mContext;

        public EfficientAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContext = context;
        }

        @Override
        public int getCount() {
            return files.length;
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
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.date = (TextView) view.findViewById(R.id.date);
                holder.details = (TextView) view.findViewById(R.id.details);
                holder.icon = (ImageView) view.findViewById(R.id.icon);
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
                holder.details.setText(displaySize(current_file.length()));
            } else if (current_file.isDirectory()) {
                File temp[] = current_file.listFiles();
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
                // Null tag means the view has the correct data
                holder.icon.setTag(null);
            } else {
                holder.icon.setImageResource(R.drawable.loading);
                // Non-null tag means the view still needs to load it's data
                holder.icon.setTag(this);
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
        private int maxSize;

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
                File f[] = new File[filterResults.count];
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
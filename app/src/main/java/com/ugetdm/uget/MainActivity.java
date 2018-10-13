package com.ugetdm.uget;

import java.io.File;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import com.ugetdm.uget.lib.*;

import android.Manifest;
import android.app.Dialog;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.webkit.MimeTypeMap;
import android.widget.*;
import android.view.ViewGroup.LayoutParams;
import android.util.DisplayMetrics;
import android.util.Log;

import ar.com.daidalos.afiledialog.FileChooserDialog;

public class MainActivity extends Activity {
    // Application data
    public static MainApp app = null;
//  public static boolean toBack = false;
    public Spinner   stateSpinner;
    public Spinner   categorySpinner;
    public ListView  stateListView;
    public ListView  categoryListView;
    public ListView  downloadListView;
    public ActionBar actionBar;

    // menu item index
    public static final class MenuMain {
        public static final int category    = 0;
        public static final int download    = 1;
        public static final int resume_all  = 2;
        public static final int pause_all  = 3;
        public static final int settings    = 4;
        public static final int about       = 5;
        public static final int exit        = 6;
    }

    public static final class MenuHomeButton {
        public static final int seqBatch    = 0;
        public static final int openCategory  = 1;
        public static final int saveCategory  = 2;
        public static final int saveAll       = 3;
        public static final int offlineMode   = 4;
    }

    public static final class MenuCategory {
        public static final int create      = 0;
        public static final int delete      = 1;
        public static final int moveUp      = 2;
        public static final int moveDown    = 3;
        public static final int preferences = 4;
    }

    public static final class MenuDownload {
        public static final int open        = 0;
        public static final int create      = 1;
        public static final int delete      = 2;
        public static final int forceStart  = 3;
        public static final int runnable    = 4;
        public static final int pause       = 5;
        public static final int move        = 6;
        public static final int priority    = 7;
        public static final int preferences = 8;

        public static final class Delete {
            public static final int recycle = 0;
            public static final int data    = 1;
            public static final int file    = 2;
        }
        public static final class Move {
            public static final int up     = 0;
            public static final int down   = 1;
        }
        public static final class Priority {
            public static final int high   = 0;
            public static final int normal = 1;
            public static final int low    = 2;
        }
    };

    // --------------------------------
    // entire lifetime: ORIENTATION

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = (MainApp)getApplicationContext();
        app.logAppend("MainActivity.onCreate()");
        app.startRunning();

        // ActionBar
        actionBar = getActionBar();
        actionBar.setHomeButtonEnabled(true);

//      if (savedInstanceState == null)
//          Log.v ("uGet", "MainActivity.onCreate()");

        registerForContextMenu(findViewById(R.id.activity_main));
//      registerForContextMenu(findViewById(R.id.download_list_empty));

        // for phone and tablet device
        app.logAppend("MainActivity.onCreate() initDownloadList");
        initDownloadList();
//      registerForContextMenu(downloadListView);
        // for tablet device
        app.logAppend("MainActivity.onCreate() initForLandscape");
        initForLandscape();
        // for phone device
        app.logAppend("MainActivity.onCreate() initForPortrait");
        initForPortrait();
        //
        app.logAppend("MainActivity.onCreate() switchDownloadAdapter");
        app.switchDownloadAdapter();

        app.logAppend("MainActivity.onCreate() initTimeoutHandler");
        initTimeoutHandler();

        processUriFromIntent();

        app.mainActivity = this;
        app.logAppend("MainActivity.onCreate() return.");
/*
if(myCurrentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
{
    // code to do for Portrait Mode
} else {
    // code to do for Landscape Mode
}
        try {
            // Waiting App until it initialized.
            while (app.isReady == false)
            Thread.sleep(125);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }

        // wait App.onCreate()
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (delayedInit())
                    this.cancel();
            }
        }, 0, 125);
 */

    }

    @Override
    protected void onDestroy() {
        app.logAppend("MainActivity.onDestroy()");
        app.mainActivity = null;
        app.adManager.destroy(adView);

        super.onDestroy();
    }

    // --------------------------------
    // visible lifetime

    @Override
    protected void onStart() {
        super.onStart();

        downloadListView.setSelection(app.nthDownloadVisible);
//        downloadListView.scrollTo(app.downloadListScrolledX, app.downloadListScrolledY);
        checkPermission();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // offline status
        app.saveStatus();

        app.nthDownloadVisible = downloadListView.getFirstVisiblePosition();
//        app.downloadListScrolledX = downloadListView.getScrollX();
//        app.downloadListScrolledY = downloadListView.getScrollY();
    }

    // --------------------------------
    // foreground lifetime

    @Override
    protected void onResume() {
        app.logAppend("MainActivity.onResume()");
        app.adManager.resume(adView);
        super.onResume();

        app.logAppend("MainActivity.onResume() switchCnodeForPortrait");
        switchCnodeForPortrait();
        app.logAppend("MainActivity.onResume() switchCnodeForLandscape");
        switchCnodeForLandscape();
        app.logAppend("MainActivity.onResume() syncCursorPosition");
        syncCursorPosition();
        app.stateAdapter.notifyDataSetChanged();

        app.logAppend("MainActivity.onResume() checkSetting");
        checkSetting();
    }

    @Override
    protected void onPause() {
        app.logAppend("MainActivity.onPause()");
        app.saveAllData();
        app.adManager.pause(adView);
        super.onPause();
    }

    // --------------------------------
    // <uses-permission  android:name="android.permission.CHANGE_CONFIGURATION">
    // </uses-permission>
    // <Activity  android:configChanges="orientation|screenSize|keyboard">
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
//      newConfig.orientation
//      Configuration.ORIENTATION_LANDSCAPE
        super.onConfigurationChanged(newConfig);
        resetAd();
    }

    // --------------------------------
    // other

    public void processUriFromIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        // if this is from the share menu
        if (Intent.ACTION_SEND.equals(action) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String uri = intent.getStringExtra(Intent.EXTRA_TEXT);
            // clear processed intent
            intent.removeExtra(Intent.EXTRA_TEXT);

            if (uri != null) {
                if (app.setting.ui.skipExistingUri && app.core.isUriExist(uri.toString()) == true)
                    return;
                // match
                long cnode = app.core.matchCategory(uri.toString(), null);
                if (cnode == 0)
                    cnode = Node.getNthChild(app.core.nodeReal, 0);
                if (cnode != 0)
                    app.core.addDownloadByUri(uri.toString(), cnode, true);
                // moveTaskToBack(true);
            }
        }
    }

    // onNewIntent -> onRestart -> onStart ->onResume
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // store the new intent unless getIntent() will return the old one
        setIntent(intent);
        processUriFromIntent();
    }

    @Override
    public void onBackPressed() {
        if (app.setting.ui.exitOnBack) {
            if (app.setting.ui.confirmExit) {
                confirmExit();
                return;
            }
        }
        else {
            // onKeyDown(KeyEvent.KEYCODE_HOME, null);
            moveTaskToBack(true);
            return;
        }

        super.onBackPressed();
    }

    // --------------------------------
    // ContextMenu

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context, contextMenu);
        // contextMenu.setHeaderIcon(android.R.drawable.stat_sys_download);
        contextMenu.setHeaderTitle(R.string.menu_context);

        super.onCreateContextMenu(contextMenu, view, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_download_new:
                openDownloadForm(true, false);
                break;

            case R.id.menu_categoey_new:
                openCategoryForm(true);
                break;

            case R.id.menu_settings:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;
        }

        return super.onContextItemSelected(item);
    }

    /*
    // for "Empty space"
    @Override
    public boolean onTouchEvent(MotionEvent me){
        if(me.getAction() == MotionEvent.ACTION_DOWN){
            Toast.makeText(this, "Empty space", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
    */

    // --------------------------------
    // OptionsMenu (in ActionBar)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View      anchorView;

        switch (item.getItemId()) {
            case android.R.id.home:
                showHomeButtonMenu(findViewById(R.id.home_button_menu_anchor));
                break;

            case R.id.menu_about:
                int myVerCode = 0;
                String myVerName = null;

                try {
                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    myVerCode = packageInfo.versionCode;
                    myVerName = packageInfo.versionName;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle(R.string.menu_about);
                dialog.setIcon(android.R.drawable.ic_dialog_info);
                dialog.setMessage(getString(R.string.app_name) + " for Android " + myVerName);
                dialog.setPositiveButton(android.R.string.ok,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }
                );
                dialog.show();
                break;

            case R.id.menu_exit:
                if (app.setting.ui.confirmExit)
                    confirmExit();
                else {
                    finish();
                    app.onTerminate();
                }
                break;

            case R.id.menu_settings:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;

            case R.id.menu_pause_all:
                app.core.pauseCategories();
                app.userAction = true;
                app.downloadAdapter.notifyDataSetChanged();
                break;

            case R.id.menu_resume_all:
                app.core.resumeCategories();
                app.downloadAdapter.notifyDataSetChanged();
                break;

            case R.id.menu_category:
                anchorView = findViewById(R.id.menu_category);
                if (anchorView == null)
                    anchorView = findViewById(R.id.main_menu_anchor);
                showCategoryMenu(anchorView);
                break;

            case R.id.menu_download:
                anchorView = findViewById(R.id.menu_download);
                if (anchorView == null)
                    anchorView = findViewById(R.id.main_menu_anchor);
                showDownloadMenu(anchorView);
                break;

            default:
                return false;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showHomeButtonMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        Menu      menu;

        popupMenu.inflate(R.menu.action_button);
        menu = popupMenu.getMenu();

        popupMenu.setOnMenuItemClickListener(
            new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    FileChooserDialog fcDialog;

                    switch (item.getItemId()) {
                    case R.id.menu_batch_sequence:
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, SequenceActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putInt("nthCategory", app.nthCategory);
                        intent.putExtras(bundle);
                        startActivity(intent);
                        break;

                    case R.id.menu_open_category:
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            runFileChooser();
                        else {
                            fcDialog = new FileChooserDialog(MainActivity.this);
                            fcDialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
                                public void onFileSelected(Dialog source, File file) {
                                    source.hide();
                                    if (app.core.loadCategory(file.getAbsolutePath()) != 0) {
                                        app.categoryAdapter.notifyDataSetChanged();
                                        app.stateAdapter.notifyDataSetChanged();
                                    }
                                }
                                // this is called when a file is created
                                public void onFileSelected(Dialog source, File folder, String name) {
                                    source.hide();
                                }
                            });
                            fcDialog.setFilter(".*json|.*JSON");
                            fcDialog.show();
                        }
                        break;

                    case R.id.menu_save_category:
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                            runFileCreator();
                        else {
                            fcDialog = new FileChooserDialog(MainActivity.this);
                            fcDialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
                                public void onFileSelected(Dialog source, File file) {
                                    source.hide();
                                    app.saveNthCategory(app.nthCategory, file.getAbsolutePath());
                                }
                                // this is called when a file is created
                                public void onFileSelected(Dialog source, File folder, String name) {
                                    source.hide();
                                    if (name.endsWith(".json") == false && name.endsWith(".JSON") == false)
                                        name = name + ".json";
                                    app.saveNthCategory(app.nthCategory, folder.getAbsolutePath() + '/' + name);
                                }
                            });
                            fcDialog.setCanCreateFiles(true);
                            fcDialog.setFilter(".*json|.*JSON");
                            fcDialog.show();
                        }
                        break;

                        case R.id.menu_save_all:
                            app.saveAllData();
                            break;

                        case R.id.menu_offline_mode:
                            if (app.setting.offlineMode)
                                app.setting.offlineMode = false;
                            else
                                app.setting.offlineMode = true;
                            break;

                        default:
                    }
                    return false;
                }
            }
        );

        // Offline Mode
        MenuItem popupItem = popupMenu.getMenu().getItem(MenuHomeButton.offlineMode);
        popupItem.setChecked(app.setting.offlineMode);
        if (app.setting.ui.noWifiGoOffline)
            popupItem.setEnabled(false);

        popupMenu.show();
    }

    // ------------------------------------------------------------------------
    // Download ListView for phone and tablet device

    private void initDownloadList() {
        downloadListView = (ListView) findViewById(R.id.download_listview);
        downloadListView.setAdapter(app.downloadAdapter);

//      downloadListView.setLongClickable(true);
//      downloadListView.setOnLongClickListener(
//          new ListView.OnLongClickListener() {
//              @Override
//              public boolean onLongClick(View view) {
//                  openContextMenu(view);
//                  showDownloadMenu(view);
//                  return true;
//              }
//          }
//      );

        downloadListView.setOnItemClickListener(
            new ListView.OnItemClickListener() {
                @Override
                public void
                onItemClick (AdapterView<?> adapterView, View view, int position, long id) {
                    app.nthDownload = position;
                    showDownloadMenu(findViewById(R.id.main_menu_anchor));
//                  showDownloadMenu(view);
                }
            }
        );
/*
        downloadListView.setOnItemLongClickListener(
            new ListView.OnItemLongClickListener() {
                @Override
                public boolean
                onItemLongClick (AdapterView<?> adapterView, View view, int position, long id) {
                    app.nthDownload = position;
                    syncCursorPosition();
//                  showDownloadMenu(view);
                    showDownloadMenu(findViewById(R.id.main_menu_anchor));
                    return true;
                }
            }
        );
*/
    }

    public void syncCursorPosition() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (app.nthDownload == -1)
                    downloadListView.clearChoices();
                else
                    downloadListView.setItemChecked(app.nthDownload, true);
            }
        });
        /*
        downloadListView.post(new Runnable() {
            @Override
            public void run() {
                if (app.nthDownload == -1)
                    downloadListView.clearChoices();
                else
                    downloadListView.setItemChecked(app.nthDownload, true);
            }
        });
        */
    }

    public void scrollToNthDownload(int nthDownload, boolean smooth) {
        if (smooth) {
            // the display will scroll to the index you want
            downloadListView.smoothScrollToPosition(app.nthDownload);
        }
        else {
            // the display will jump to the index you want
            downloadListView.setSelection(app.nthDownload);
        }
    }

    public boolean isNthDownloadVisible(int nthDownload) {
        int visibleRangeBeg, visibleRangeEnd;
        visibleRangeBeg = downloadListView.getFirstVisiblePosition();
        visibleRangeEnd = visibleRangeBeg + downloadListView.getChildCount();
        if (nthDownload > visibleRangeBeg && nthDownload <= visibleRangeEnd)
            return true;
        else
            return false;
    }

    // --------------------------------
    // Download Form & Menu

    private void openDownloadForm(boolean isCreation, boolean isMultiple) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, NodeActivity.class);
        Bundle bundle = new Bundle();
        long cnodePointer;
        long dnodePointer;

        cnodePointer = app.downloadAdapter.pointer;

        if (isCreation) {
            Download downloadData;

            downloadData = new Download();
            dnodePointer = Node.create();

            // if current category is fake one, use setting from real category.
            if (app.nthCategory == 0)
                cnodePointer = Node.getNthChild(app.core.nodeReal, 0);

            Data.get(Node.data(cnodePointer), downloadData);
            Data.set(Node.data(dnodePointer), downloadData);
            downloadData = null;
        }
        else {
            dnodePointer = Node.getNthChild(cnodePointer, app.nthDownload);
            if (dnodePointer == 0)
                return;
        }

        bundle.putLong("NodePointer", dnodePointer);
        bundle.putInt("nthCategory", app.nthCategory);
        bundle.putBoolean("isCategory", false);
        bundle.putBoolean("isCreation", isCreation);
        bundle.putBoolean("isMultiple", isMultiple);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void showDownloadMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.download);

        popupMenu.setOnMenuItemClickListener(
            new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    long  dNodePointer;

                    switch (item.getItemId()) {
                    case R.id.menu_download_open:
                        File file = app.getDownloadedFile(app.nthDownload);
                        if (file != null) {
                            // create Intent for Activity
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                Uri uri = Uri.fromFile(file);
                                String url = uri.toString();

                                // grab mime type
                                String newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                        MimeTypeMap.getFileExtensionFromUrl(url));

                                intent.setDataAndType(uri, newMimeType);
                                startActivity (intent);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        else {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
//                          dialog.setTitle(R.string.menu_download_open);
                            dialog.setIcon(android.R.drawable.ic_dialog_alert);
                            dialog.setMessage(R.string.message_file_not_exist);
                            dialog.setPositiveButton(android.R.string.cancel, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            dialog.show();
                        }
                        break;

                    case R.id.menu_download_new:
                        openDownloadForm(true, false);
                        break;

                    case R.id.menu_download_delete_recycle:
                        dNodePointer = app.getNthDownloadNode(app.nthDownload);
                        app.recycleNthDownload(app.nthDownload);
                        app.setSelectedDownload(dNodePointer);
                        syncCursorPosition();
//                        scrollToNthDownload(app.nthDownload, true);
                        break;

                    case R.id.menu_download_delete_data:
                        app.deleteNthDownload(app.nthDownload, false);
                        app.nthDownload = -1;
                        break;

                    case R.id.menu_download_delete_file:
                        if (app.setting.ui.confirmDelete)
                            confirmDelete();
                        else {
                            app.deleteNthDownload(app.nthDownload, true);
                            app.nthDownload = -1;
                        }
                        break;

                    case R.id.menu_download_force_start:
                        dNodePointer = app.getNthDownloadNode(app.nthDownload);
                        app.activateNthDownload(app.nthDownload);
                        app.setSelectedDownload(dNodePointer);
                        syncCursorPosition();
//                        scrollToNthDownload(app.nthDownload, true);
                        break;

                    case R.id.menu_download_runnable:
                        dNodePointer = app.getNthDownloadNode(app.nthDownload);
                        if (app.setNthDownloadRunnable(app.nthDownload) == false)
                            break;
                        app.setSelectedDownload(dNodePointer);
                        syncCursorPosition();
//                        scrollToNthDownload(app.nthDownload, true);
                        break;

                    case R.id.menu_download_pause:
                        dNodePointer = app.getNthDownloadNode(app.nthDownload);
                        app.pauseNthDownload(app.nthDownload);
                        app.setSelectedDownload(dNodePointer);
                        syncCursorPosition();
//                        scrollToNthDownload(app.nthDownload, true);
                        break;

                    case R.id.menu_download_move_up:
                        if (app.moveNthDownload(app.nthDownload, app.nthDownload -1)) {
                            app.nthDownload--;
                            syncCursorPosition();
//                            scrollToNthDownload(app.nthDownload, true);
                        }
                        break;

                    case R.id.menu_download_move_down:
                        if (app.moveNthDownload(app.nthDownload, app.nthDownload +1)) {
                            app.nthDownload++;
                            syncCursorPosition();
//                            scrollToNthDownload(app.nthDownload, true);
                        }
                        break;

                    case R.id.menu_download_priority_high:
                        app.setNthDownloadPriority(app.nthDownload, Core.Priority.high);
                        break;

                    case R.id.menu_download_priority_normal:
                        app.setNthDownloadPriority(app.nthDownload, Core.Priority.normal);
                        break;

                    case R.id.menu_download_priority_low:
                        app.setNthDownloadPriority(app.nthDownload, Core.Priority.low);
                        break;

                    case R.id.menu_download_preferences:
                        openDownloadForm(false, false);
                        break;
                    }
                    // end of switch (item.getItemId())
                    return true;
                }
            }
        );

        Menu menu = popupMenu.getMenu();

        if (app.nthDownload == -1) {
            menu.getItem(MenuDownload.open).setEnabled(false);
            menu.getItem(MenuDownload.delete).setEnabled(false);
            menu.getItem(MenuDownload.forceStart).setEnabled(false);
            menu.getItem(MenuDownload.runnable).setEnabled(false);
            menu.getItem(MenuDownload.pause).setEnabled(false);
            menu.getItem(MenuDownload.move).setEnabled(false);
            menu.getItem(MenuDownload.priority).setEnabled(false).setVisible(false);
            menu.getItem(MenuDownload.preferences).setEnabled(false).setVisible(false);
        }
        else {
            // Any Category/Status can't move download position if they were sorted.
            if (app.setting.sortBy > 0) {
                menu.getItem(MenuDownload.move).setEnabled(false);
            }
            else {
                if (app.nthDownload == 0) {
                    menu.getItem(MenuDownload.move).getSubMenu()
                            .getItem(MenuDownload.Move.up).setEnabled(false);
                }
                if (app.nthDownload == Node.nChildren(app.downloadAdapter.pointer) -1) {
                    menu.getItem(MenuDownload.move).getSubMenu()
                            .getItem(MenuDownload.Move.down).setEnabled(false);
                }
            }
            // priority
            int  priority = app.getNthDownloadPriority(app.nthDownload);
            switch (priority) {
            case Core.Priority.high:
                menu.getItem(MenuDownload.priority).getSubMenu()
                        .getItem(MenuDownload.Priority.high).setChecked(true);
                break;

            case Core.Priority.normal:
                menu.getItem(MenuDownload.priority).getSubMenu()
                        .getItem(MenuDownload.Priority.normal).setChecked(true);
                break;

            case Core.Priority.low:
                menu.getItem(MenuDownload.priority).getSubMenu()
                        .getItem(MenuDownload.Priority.low).setChecked(true);
                break;
            }
        }

        // if file doesn't exist, disable menu item: "open" and "delete file".
        if (app.getDownloadedFile(app.nthDownload) == null) {
            menu.getItem(MenuDownload.open).setEnabled(false);
            menu.getItem(MenuDownload.delete).getSubMenu()
                    .getItem(MenuDownload.Delete.file).setEnabled(false);
        }

        if (app.nthStatus == 1) {
            menu.getItem(MenuDownload.forceStart).setEnabled(false);
            menu.getItem(MenuDownload.runnable).setEnabled(false);
            menu.getItem(MenuDownload.preferences).setEnabled(false);
        }
        if (app.nthStatus > 2) {
            menu.getItem(MenuDownload.pause).setEnabled(false);
        }

        popupMenu.show();
    }

    // --------------------------------
    // Category Form & Menu

    private void openCategoryForm(boolean isCreation) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, NodeActivity.class);
        Bundle bundle = new Bundle();

        long cPointer;

        if (app.nthCategory == 0)
            cPointer = Node.getNthChild(app.core.nodeReal, 0);
        else
            cPointer = app.downloadAdapter.pointer;
//          cPointer = Node.getNthChild(app.nodeReal, app.nthCategory - 1);

        if (isCreation) {
            long      newPointer;
            Category  categoryData;

            categoryData = new Category();
            newPointer = Node.create();
            Data.get(Node.data(cPointer), categoryData);
            categoryData.name = "Copy of " + categoryData.name;
            Data.set(Node.data(newPointer), categoryData);
            cPointer = newPointer;
            categoryData = null;
        }

        bundle.putLong("NodePointer", cPointer);
        bundle.putBoolean("isCategory", true);
        bundle.putBoolean("isMultiple", false);
        bundle.putBoolean("isCreation", isCreation);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void showCategoryMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        Menu      menu;

        popupMenu.inflate(R.menu.category);
        menu = popupMenu.getMenu();
//      menu.setHeaderIcon(android.R.drawable.stat_sys_download);
//      menu.setHeaderTitle(R.string.download_setting);

        // disable Delete and Preferences
        if (app.nthCategory < 1) {
            menu.getItem(MenuCategory.delete).setEnabled(false);
//          menu.getItem(MenuCategory.preferences).setEnabled(false);
        }
        // disable Move Up/Down
        if (app.nthCategory == 0 || app.nthCategory == Node.nChildren(app.core.nodeReal))
            menu.getItem(MenuCategory.moveDown).setEnabled(false);
        if (app.nthCategory < 2)
            menu.getItem(MenuCategory.moveUp).setEnabled(false);

        popupMenu.setOnMenuItemClickListener(
            new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                    case R.id.menu_category_new:
                        openCategoryForm(true);
                        break;

                    case R.id.menu_category_delete:
                        confirmDeleteCategory();
//                      app.deleteNthCategory(app.nthCategory);
                        break;

                    case R.id.menu_category_move_up:
                        if (app.moveNthCategory(app.nthCategory, app.nthCategory -1) == true) {
                            switchCnodeForLandscape();
                            switchCnodeForPortrait();
                        }
                        break;

                    case R.id.menu_category_move_down:
                        if (app.moveNthCategory(app.nthCategory, app.nthCategory +1) == true) {
                            switchCnodeForLandscape();
                            switchCnodeForPortrait();
                        }
                        break;

                    case R.id.menu_category_preferences:
                        openCategoryForm(false);
                        break;

                    default:
                    }
                    return false;
                }
            }
        );

        popupMenu.show();
    }

    // ------------------------------------------------------------------------
    // Category ListView - for tablet device

    private void initForLandscape() {
        stateListView = (ListView) findViewById(R.id.state_listview);
        categoryListView = (ListView) findViewById(R.id.category_listview);
        if (stateListView == null || categoryListView == null)
            return;

        stateListView.setOnItemClickListener(
            new ListView.OnItemClickListener() {
                @Override
                public void
                onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    app.nthStatus = position;
                    app.switchDownloadAdapter();
                    syncCursorPosition();
                }
            }
        );

        categoryListView.setOnItemClickListener(
            new ListView.OnItemClickListener() {
                @Override
                public void
                onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    app.nthCategory = position;
                    app.switchDownloadAdapter();
                    syncCursorPosition();
                }
            }
        );

        categoryListView.setOnItemLongClickListener(
            new ListView.OnItemLongClickListener() {
                @Override
                public boolean
                onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                    app.nthCategory = position;
                    app.switchDownloadAdapter();
                    syncCursorPosition();
                    switchCnodeForLandscape();
                    showCategoryMenu(view);
                    return true;
                }
            }
        );

    }

    private void switchCnodeForLandscape() {
        if (stateListView == null || categoryListView == null)
            return;

        categoryListView.clearFocus();
        categoryListView.post(new Runnable() {
            @Override
            public void run() {
                categoryListView.setItemChecked(app.nthCategory, true);
//              categoryListView.setSelection(app.nthCategory);
            }
        });

        stateListView.clearFocus();
        stateListView.post(new Runnable() {
            @Override
            public void run() {
                stateListView.setItemChecked(app.nthStatus, true);
//              stateListView.setSelection(app.nthStatus);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Category Spinner - for phone device

    private void initForPortrait() {
        stateSpinner = (Spinner) findViewById(R.id.state_spinner);
        categorySpinner = (Spinner) findViewById(R.id.category_spinner);
        if (stateSpinner == null || categorySpinner == null)
            return;
        stateSpinner.setAdapter(app.stateAdapter);
        categorySpinner.setAdapter(app.categoryAdapter);

        stateSpinner.setOnItemSelectedListener(
            new Spinner.OnItemSelectedListener() {
                @Override
                public void
                onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    app.nthStatus = position;
                    app.switchDownloadAdapter();
                    syncCursorPosition();
                }

                @Override
                public void
                onNothingSelected(AdapterView<?> arg0) {}
            }
        );

        categorySpinner.setOnItemSelectedListener(
            new Spinner.OnItemSelectedListener() {
                @Override
                public void
                onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    app.nthCategory = position;
                    app.switchDownloadAdapter();
                    syncCursorPosition();
                }

                @Override
                public void
                onNothingSelected(AdapterView<?> arg0) {}
            }
        );

        categorySpinner.setLongClickable(true);
        categorySpinner.setOnLongClickListener(
            new Spinner.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // TODO Auto-generated method stub
                    showCategoryMenu(view);
                    return true;
                }
            }
        );
    }

    private void switchCnodeForPortrait() {
        if (categorySpinner != null)
            categorySpinner.setSelection(app.nthCategory);
        if (stateSpinner != null)
            stateSpinner.setSelection(app.nthStatus);
    }

    // ------------------------------------------------------------------------
    // confirm & dialog

    public void confirmDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getResources().getString(R.string.dialog_confirm_delete));
        builder.setTitle(getResources().getString(R.string.dialog_confirm_delete_title));
        builder.setPositiveButton(getResources().getString(android.R.string.yes), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                app.deleteNthDownload(app.nthDownload, true);
                app.nthDownload = -1;
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getResources().getString(android.R.string.no), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    public void confirmDeleteCategory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getResources().getString(R.string.dialog_confirm_delete_category));
        builder.setTitle(getResources().getString(R.string.dialog_confirm_delete_category_title));
        builder.setPositiveButton(getResources().getString(android.R.string.yes), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                app.deleteNthCategory(app.nthCategory);
                switchCnodeForLandscape();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getResources().getString(android.R.string.no), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    public void confirmExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(getResources().getString(R.string.dialog_confirm_exit));
        builder.setTitle(getResources().getString(R.string.dialog_confirm_exit_title));
        builder.setPositiveButton(getResources().getString(android.R.string.yes), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
                app.onTerminate();
            }
        });

        builder.setNegativeButton(getResources().getString(android.R.string.no), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    // ------------------------------------------------------------------------
    // check setting
    public void checkSetting() {
        // test clipboard type pattern
        try {
            String string = new String("test string");
            string.matches(app.setting.clipboard.types);
        }
        catch (PatternSyntaxException e) {
            showMessage (getString(R.string.preference_clipboard_type_error_title),
                    getString(R.string.preference_clipboard_type_error_message));
            return;
        }
    }

    // ------------------------------------------------------------------------
    // Message
    public void showMessage(String title, String msg)
    {
        AlertDialog.Builder MyAlertDialog = new AlertDialog.Builder(this);
        MyAlertDialog.setTitle(title);
        MyAlertDialog.setMessage(msg);

        DialogInterface.OnClickListener OkClick = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        };
        MyAlertDialog.setNeutralButton(getResources().getString(android.R.string.ok), OkClick);
        MyAlertDialog.show();
    }

/*
    // ------------------------------------------------------------------------
    // Screen Size
    void getScreenSize() {
        int  width, height;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width = dm.widthPixels;
        height = dm.heightPixels;
    }
*/

    // --------------------------------
    // permission

    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int FILE_CHOOSER_CODE = 42;
    private static final int FILE_CREATOR_CODE = 43;

    protected void checkPermission () {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Permission to access the SD-CARD is required for this app to Download files.")
                        .setTitle("Permission required");

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        makeRequest();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
            else {
                makeRequest();
            }
        }
    }

    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_STORAGE);
    }

    protected void runFileChooser() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.putExtra(Intent.EXTRA_TITLE, "Pick category json file");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/*");
            // Android doesn't support 'json', getMimeTypeFromExtension("json") return null
            // intent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip"));
            startActivityForResult(intent, FILE_CHOOSER_CODE);
        }
    }

    protected void onFileChooserResult(Uri treeUri) {
        ParcelFileDescriptor parcelFD;

        grantUriPermission(getPackageName(), treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            parcelFD = getContentResolver().openFileDescriptor(treeUri, "r");
        }
        catch (Exception e) {
            parcelFD = null;
        }

        String filename = DocumentFile.fromSingleUri(this, treeUri).getName();
        if (parcelFD != null && app.core.loadCategory(parcelFD.detachFd()) != 0) {
            app.categoryAdapter.notifyDataSetChanged();
            app.stateAdapter.notifyDataSetChanged();
            Toast.makeText(this, "load " + filename,
                    Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Failed to load " + filename,
                    Toast.LENGTH_SHORT).show();
        }

        revokeUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    protected void runFileCreator() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/*");
            // Android doesn't support 'json', getMimeTypeFromExtension("json") return null
            startActivityForResult(intent, FILE_CREATOR_CODE);
        }
    }

    protected void onFileCreatorResult(Uri treeUri) {
        ParcelFileDescriptor parcelFD;

        grantUriPermission(getPackageName(), treeUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            parcelFD = getContentResolver().openFileDescriptor(treeUri, "w");
        }
        catch (Exception e) {
            parcelFD = null;
        }

        String filename = DocumentFile.fromSingleUri(this, treeUri).getName();
        if (parcelFD != null && app.saveNthCategory(app.nthCategory, parcelFD.detachFd()) != false) {
            Toast.makeText(this, "save " + filename,
                    Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Failed to save " + filename,
                    Toast.LENGTH_SHORT).show();
        }

        revokeUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    //  @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_CANCELED)
            return;
        Uri  treeUri = resultData.getData(); // you can't use Uri.fromFile() to get path

        DocumentFile docFile = DocumentFile.fromSingleUri(this, treeUri);
        String name = docFile.getName();

        switch (requestCode) {
            case FILE_CHOOSER_CODE:
                onFileChooserResult(treeUri);
                break;

            case FILE_CREATOR_CODE:
                onFileCreatorResult(treeUri);
                break;

            default:
                break;
        }
    }

    // ------------------------------------------------------------------------
    // Timeout Interval & Handler

    private static final int speedInterval = 1000;
    private Handler  speedHandler  = new Handler();
    private Runnable speedRunnable = new Runnable() {
        @Override
        public void run() {
            // show speed in subtitle
            if (app.core.downloadSpeed == 0 && app.core.uploadSpeed == 0)
                actionBar.setSubtitle(null);
            else {
                String string = "";
                if (app.core.downloadSpeed > 0)
                    string += "↓ " + Util.stringFromIntUnit(app.core.downloadSpeed, 1);
                if (app.core.uploadSpeed > 0) {
                    if (app.core.downloadSpeed > 0)
                        string += " , ";
                    string += "↑ " + Util.stringFromIntUnit(app.core.uploadSpeed, 1);
                }
                actionBar.setSubtitle(string);
                string = null;
            }

            // show offline status in title
            if (app.setting.offlineMode == false)
                actionBar.setTitle(getString(R.string.app_name));
            else {
                actionBar.setTitle(getString(R.string.app_name) + " " +
                        getString(R.string.app_offline));
            }


            // call this function after the specified time interval
            speedHandler.postDelayed(this, speedInterval);
        }
    };

    public void initTimeoutHandler() {
        speedHandler.postDelayed(speedRunnable, speedInterval);
        speedHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initAd();
            }
        }, 1000);
    }

    // ------------------------------------------------------------------------
    // Ad
    View  adView = null;

    public void resetAd () {
        app.adManager.remove(adView);
        initAd();
    }

    public void initAd() {
        double ratio;
        int    viewWidthDp;

        // configuration.smallestScreenWidthDp;
        // configuration.screenWidthDp;
        Configuration configuration = getResources().getConfiguration();
        // if Landscape else Portrait
        if (configuration.smallestScreenWidthDp < configuration.screenWidthDp)
            viewWidthDp = configuration.screenWidthDp * 2 / 3;
        else
            viewWidthDp = 0;

        LinearLayout adParent = (LinearLayout) findViewById(R.id.linearAd);
        adView = app.adManager.add(
                adParent,
                this, viewWidthDp);
    }
}


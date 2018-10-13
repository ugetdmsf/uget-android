package com.ugetdm.uget;

import com.google.android.gms.ads.AdRequest;
import com.ugetdm.uget.lib.*;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;

import java.io.File;
import java.util.List;

import ar.com.daidalos.afiledialog.*;

public class NodeActivity extends Activity {
	private MainApp     app;
	private TabHost     tabHost;
//	private TabWidget   tabWidget;
	private Bundle      bundle;
	private long        nodePointer;
	private long        dataPointer;
	private boolean     isCategory;
	private boolean     isCreation;
	private boolean     isMultiple;
//	private boolean     filenameChanged = false;
	private Category    categoryData;
	//static
	private static int    nthCategoryReal = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_node);

		app = (MainApp)getApplicationContext();

		categoryData = new Category();
		bundle = getIntent().getExtras();
		if (nthCategoryReal == -1) {
			nthCategoryReal = bundle.getInt("nthCategory", 0) - 1;
			if (nthCategoryReal < 0)
				nthCategoryReal = 0;
		}
        nodePointer = bundle.getLong("NodePointer");
		isCategory = bundle.getBoolean("isCategory");
		isCreation = bundle.getBoolean("isCreation");
		isMultiple = bundle.getBoolean("isMultiple");
		dataPointer = Node.data(nodePointer);
		Data.get(dataPointer, categoryData);
		Data.ref(dataPointer);

		initView();
        initAd();
		setSetting(categoryData, false);
	}

	@Override
	protected void onDestroy() {
		nthCategoryReal = -1;  // reset this static value
		super.onDestroy();
		Data.unref(dataPointer);
        app.adManager.destroy(adView);
    }

    @Override
    protected void onResume() {
        super.onResume();
		app.adManager.resume(adView);
    }

    @Override
    protected void onPause() {
        super.onPause();
		app.adManager.pause(adView);
    }

	// --------------------------------
	// <uses-permission  android:name="android.permission.CHANGE_CONFIGURATION">
	// </uses-permission>
	// <Activity  android:configChanges="orientation|keyboard">
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
//      newConfig.orientation
//      Configuration.ORIENTATION_LANDSCAPE
		super.onConfigurationChanged(newConfig);
//		resetAd();
	}

    // --------------------------------
	// OptionsMenu

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		if (isCategory || isMultiple || isCreation == false)
			getMenuInflater().inflate(R.menu.action_node, menu);
		else
			getMenuInflater().inflate(R.menu.action_dnode, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.menu_category:
				View      anchorView;
				PopupMenu popupMenu;
				Menu      menu;

				anchorView = findViewById(R.id.menu_category);
				if (anchorView == null)
					anchorView = findViewById(R.id.menu_action_ok);
				popupMenu = new PopupMenu(this, anchorView);
				menu = popupMenu.getMenu();

				int  nItem = Node.nChildren(app.core.nodeReal);
				for (int index = 0; index < nItem;  index++) {
					long pointer;
					pointer = Node.getNthChild(app.core.nodeReal, index);
					pointer = Node.data(pointer);
					// groupId,  itemId,  order,  string
					menu.add(0, index, index, Data.getName(pointer)).setChecked(index == nthCategoryReal);
				}
				popupMenu.setOnMenuItemClickListener(
						new PopupMenu.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								nthCategoryReal = item.getItemId();
                                categoryData = new Category();
                                long pointer = Node.getNthChild(app.core.nodeReal, nthCategoryReal);
                                if (pointer != 0) {
                                	pointer = Node.data(pointer);
                                    Data.get(pointer, categoryData);
                                    categoryData.name = null;
                                    Data.set(dataPointer, categoryData);
                                    setSetting(categoryData, true);
                                }
								return true;
							}
						}
				);
				menu.setGroupCheckable(0, true, true);  // groupId,  checkable,  exclusive
				popupMenu.show();
				break;

		case R.id.menu_action_ok:
			onClickOK();
			break;

		case R.id.menu_action_startup_mode:
			showStartupModeMenu(findViewById(R.id.menu_action_ok));
			break;

		default:
		}
		return super.onOptionsItemSelected(item);
	}

	// --------------------------------

	protected void initView() {
		Resources resources = getResources();

		tabHost = (TabHost) findViewById(R.id.node_tabhost);
		tabHost.setup ();
		tabHost.addTab(tabHost
				.newTabSpec("t1")
				.setIndicator(resources.getString(R.string.category_setting), null)
				.setContent(R.id.cnode_page));
		tabHost.addTab(tabHost
				.newTabSpec("t2")
				.setIndicator(resources.getString(R.string.download_default), null)
				.setContent(R.id.dnode_page));
		tabHost.setCurrentTab(0);

		if (isCategory == true || isMultiple == true) {
			// Disable URI
			findViewById(R.id.dnode_uri).setEnabled(false);
			findViewById(R.id.dnode_uri_editor).setEnabled(false);
			findViewById(R.id.dnode_mirrors).setEnabled(false);
			findViewById(R.id.dnode_mirrors_editor).setEnabled(false);
			findViewById(R.id.dnode_file).setEnabled(false);
			findViewById(R.id.dnode_file_editor).setEnabled(false);
		}
		else if (isCreation == false) {
			EditText editText;
			editText = (EditText) findViewById(R.id.dnode_uri_editor);
			editText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					// TODO Auto-generated method stub
				}

				@Override
				public void afterTextChanged(Editable s) {
//					filenameChanged = true;
				}
			});
		}

		if (isCategory == true) {
			if (isMultiple == true)
				findViewById(R.id.cnode_name).setEnabled(false);
		}
		else {
			tabHost.setCurrentTab(1);
			tabHost.getTabWidget().getChildTabViewAt(0).setVisibility(View.GONE);
			// hide tab bar
			tabHost.getTabWidget().setVisibility(View.GONE);
		}

		ActionBar actionBar = getActionBar();
		if (isCategory == true) {
			if (isCreation) {
				actionBar.setSubtitle(
						resources.getString(R.string.category_creation));
			}
			else {
				actionBar.setSubtitle(
						resources.getString(R.string.category_setting));
			}
		}
		else {
			if (isCreation) {
				actionBar.setSubtitle(
						resources.getString(R.string.download_creation));
			}
			else {
				actionBar.setSubtitle(
						resources.getString(R.string.download_setting));
			}
		}
		resources = null;

		// proxy type
		Spinner spinner = (Spinner)findViewById(R.id.dnode_proxy_type_spinner);
		spinner.setAdapter(new ProxyTypeAdapter(this));
		spinner.setOnItemSelectedListener(
			new Spinner.OnItemSelectedListener() {
				@Override
				public void
				onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
					if (position == 0)
						disableProxy();
					else
						enableProxy();
				}

				@Override
				public void
				onNothingSelected(AdapterView<?> arg0) {}
			}
		);

		Button button;
		button = (Button) findViewById(R.id.dnode_folder_button);
		button.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				showFolderMenu(v);
			}
		});
	}

	protected void onClickOK() {
		getSetting(categoryData);

		if (isFolderWritable(categoryData.folder) == false) {
			runFolderRequest();
			return;
		}

		if (isCategory) {
			Data.set(dataPointer, categoryData);
			if (isCreation)
				app.addCategoryAndNotify(nodePointer);
			else
				app.categoryAdapter.notifyDataSetChanged();
		}
		else {
			Data.set(dataPointer, (Download)categoryData);
			if (isCreation) {
				EditText editText;
				editText = (EditText) findViewById(R.id.dnode_uri_editor);
				if (editText.getText().toString().length() == 0) {
					showNoUriDialog();
					return;
				}
				app.addDownloadAndNotify(nodePointer, nthCategoryReal + 1);
			}
			else {
				app.core.resetDownloadName(nodePointer);
				app.downloadAdapter.notifyDataSetChanged();
			}
		}
		app.addFolderHistory(categoryData.folder);
		finish();
	}

	protected void showNoUriDialog() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setIcon(android.R.drawable.ic_dialog_alert);
		dialog.setMessage(R.string.message_no_uri);
		dialog.show();
	}

	// ------------------------------------------------------------------------
	// Startup Mode

	protected void showStartupModeMenu(View view) {
		PopupMenu popupMenu = new PopupMenu(this, view);
		popupMenu.inflate(R.menu.download_mode);

		popupMenu.setOnMenuItemClickListener(
			new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					switch (item.getItemId()) {
					case R.id.menu_download_start_auto:
						Data.setGroup(dataPointer, 0);
						break;

					case R.id.menu_download_start_manually:
						Data.setGroup(dataPointer, Node.Group.pause);
						break;
					}
					return true;
				}
			}
		);

		Menu menu  = popupMenu.getMenu();
		int  state = Data.getGroup(dataPointer);
		switch (state) {
		case Node.Group.pause:
			menu.getItem(1).setChecked(true);
			break;

		default:
			menu.getItem(0).setChecked(true);
			break;
		}
		popupMenu.show();
	}

	// ------------------------------------------------------------------------
	// proxy

	protected void enableProxy() {
		View  view;
		view = findViewById(R.id.dnode_proxy_host);
		view.setEnabled(true);
		view = findViewById(R.id.dnode_proxy_host_editor);
		view.setEnabled(true);
		view = findViewById(R.id.dnode_proxy_port);
		view.setEnabled(true);
		view = findViewById(R.id.dnode_proxy_port_editor);
		view.setEnabled(true);
		view = findViewById(R.id.dnode_proxy_user);
		view.setEnabled(true);
		view = findViewById(R.id.dnode_proxy_user_editor);
		view.setEnabled(true);
		view = findViewById(R.id.dnode_proxy_password);
		view.setEnabled(true);
		view = findViewById(R.id.dnode_proxy_password_editor);
		view.setEnabled(true);
	}

	protected void disableProxy() {
		View  view;
		view = findViewById(R.id.dnode_proxy_host);
		view.setEnabled(false);
		view = findViewById(R.id.dnode_proxy_host_editor);
		view.setEnabled(false);
		view = findViewById(R.id.dnode_proxy_port);
		view.setEnabled(false);
		view = findViewById(R.id.dnode_proxy_port_editor);
		view.setEnabled(false);
		view = findViewById(R.id.dnode_proxy_user);
		view.setEnabled(false);
		view = findViewById(R.id.dnode_proxy_user_editor);
		view.setEnabled(false);
		view = findViewById(R.id.dnode_proxy_password);
		view.setEnabled(false);
		view = findViewById(R.id.dnode_proxy_password_editor);
		view.setEnabled(false);
	}

	// ------------------------------------------------------------------------
	MenuItem  itemSelectFolder = null;

	protected void showFolderMenu(View view) {
		PopupMenu  popupMenu;
		Menu       menu;

		popupMenu = new PopupMenu(this, view);
		menu = popupMenu.getMenu();
		for (int count = 0;  count < app.folderHistory.length;  count++) {
			if (app.folderHistory[count] != null)
				menu.add(app.folderHistory[count]);
		}
		itemSelectFolder = menu.add(getString(R.string.menu_select_folder));
		itemSelectFolder.setIcon(R.mipmap.folder);

		popupMenu.setOnMenuItemClickListener(
			new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
                    if (item == itemSelectFolder) {
						itemSelectFolder = null;
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
							runFolderChooser();
						else {
							FileChooserDialog dialog = new FileChooserDialog(NodeActivity.this);
							dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
								public void onFileSelected(Dialog source, File file) {
									source.hide();
									EditText editText = (EditText) findViewById(R.id.dnode_folder_editor);
									editText.setText(file.getAbsolutePath());
								}
								public void onFileSelected(Dialog source, File folder, String name) {
									source.hide();
								}
							});

							EditText editText = (EditText) findViewById(R.id.dnode_folder_editor);
							File folder = new File(editText.getText().toString());
							if (folder.exists() && folder.isDirectory())
								dialog.loadFolder(folder.toString());
							dialog.setFolderMode(true);
							dialog.show();
						}
                    }
                    else {
                        EditText editText = (EditText) findViewById(R.id.dnode_folder_editor);
                        editText.setText(item.getTitle().toString());
                    }
					return false;
				}
			}
		);
		popupMenu.show();
	}

	protected void setUriFromClipboard() {
		EditText editText;

		editText = (EditText) findViewById(R.id.dnode_uri_editor);
		Uri  uri = app.getUriFromClipboard(false);
		if (uri != null)
			editText.setText(uri.toString());
	}

	// ------------------------------------------------------------------------

	protected void setSetting(Category categoryData, boolean keepPrivateValue) {
		EditText editText;

		if (nodePointer == 0)
			return;
//		Log.i ("NodeActivity", "folder = " + dset.folder);

		if (isCategory == true) {
			if (isMultiple == false) {
				editText = (EditText) findViewById(R.id.name_editor);
				editText.setText(categoryData.name);
			}
			editText = (EditText) findViewById(R.id.active_limit_editor);
			editText.setText(Integer.toString(categoryData.activeLimit));
			editText = (EditText) findViewById(R.id.finished_limit_editor);
			editText.setText(Integer.toString(categoryData.finishedLimit));
			editText = (EditText) findViewById(R.id.recycled_limit_editor);
			editText.setText(Integer.toString(categoryData.recycledLimit));
			editText = (EditText) findViewById(R.id.hosts_editor);
			editText.setText(categoryData.hosts);
			editText = (EditText) findViewById(R.id.schemes_editor);
			editText.setText(categoryData.schemes);
			editText = (EditText) findViewById(R.id.file_types_editor);
			editText.setText(categoryData.fileTypes);
		}

		if (isCategory == false && isMultiple == false && keepPrivateValue == false) {
			editText = (EditText) findViewById(R.id.dnode_uri_editor);
			editText.setText(categoryData.uri);
			if (isCreation == true && isCategory == false)
				setUriFromClipboard();
		}
        if (keepPrivateValue == false) {
            editText = (EditText) findViewById(R.id.dnode_mirrors_editor);
            editText.setText(categoryData.mirrors);
        }
        if (keepPrivateValue == false) {
            editText = (EditText) findViewById(R.id.dnode_file_editor);
            editText.setText(categoryData.file);
        }
		editText = (EditText) findViewById(R.id.dnode_folder_editor);
		editText.setText(categoryData.folder);
		editText = (EditText) findViewById(R.id.dnode_referrer_editor);
		editText.setText(categoryData.referrer);
		editText = (EditText) findViewById(R.id.dnode_user_editor);
		editText.setText(categoryData.user);
		editText = (EditText) findViewById(R.id.dnode_password_editor);
		editText.setText(categoryData.password);
		editText = (EditText) findViewById(R.id.dnode_connections_editor);
		editText.setText(Integer.toString(categoryData.connections));
//		editText = (EditText) findViewById(R.id.dnode_retry_editor);
//		editText.setText(Integer.toString(categoryData.retryLimit));
		// proxy
		editText = (EditText) findViewById(R.id.dnode_proxy_port_editor);
		editText.setText(Integer.toString(categoryData.proxyPort));
		editText = (EditText) findViewById(R.id.dnode_proxy_host_editor);
		editText.setText(categoryData.proxyHost);
		editText = (EditText) findViewById(R.id.dnode_proxy_user_editor);
		editText.setText(categoryData.proxyUser);
		editText = (EditText) findViewById(R.id.dnode_proxy_password_editor);
		editText.setText(categoryData.proxyPassword);
		Spinner spinner = (Spinner) findViewById(R.id.dnode_proxy_type_spinner);
		spinner.setSelection(categoryData.proxyType);
	}

	protected void getSetting(Category categoryData) {
		EditText editText;
		String   string;

		if (nodePointer == 0)
			return;

		if (isCategory == true) {
			if (isMultiple == false) {
				editText = (EditText) findViewById(R.id.name_editor);
				categoryData.name = editText.getText().toString();
			}
			// activeLimit
			editText = (EditText) findViewById(R.id.active_limit_editor);
			string = editText.getText().toString();
			if (string.length() > 0)
				categoryData.activeLimit = Integer.parseInt(string);
			else
				categoryData.activeLimit = 2;
			// finishedLimit
			editText = (EditText) findViewById(R.id.finished_limit_editor);
			string = editText.getText().toString();
			if (string.length() > 0)
				categoryData.finishedLimit = Integer.parseInt(string);
			else
				categoryData.finishedLimit = 100;
			// recycledLimit
			editText = (EditText) findViewById(R.id.recycled_limit_editor);
			string = editText.getText().toString();
			if (string.length() > 0)
				categoryData.recycledLimit = Integer.parseInt(string);
			else
				categoryData.recycledLimit = 100;

			editText = (EditText) findViewById(R.id.hosts_editor);
			categoryData.hosts = editText.getText().toString();
			editText = (EditText) findViewById(R.id.schemes_editor);
			categoryData.schemes = editText.getText().toString();
			editText = (EditText) findViewById(R.id.file_types_editor);
			categoryData.fileTypes = editText.getText().toString();
		}

		if (isCategory == false && isMultiple == false) {
			editText = (EditText) findViewById(R.id.dnode_uri_editor);
			categoryData.uri = editText.getText().toString();
		}
		editText = (EditText) findViewById(R.id.dnode_mirrors_editor);
		categoryData.mirrors = editText.getText().toString();
		editText = (EditText) findViewById(R.id.dnode_file_editor);
		categoryData.file = editText.getText().toString();

		editText = (EditText) findViewById(R.id.dnode_folder_editor);
		categoryData.folder = editText.getText().toString();
		// check folder
		int folderLength = categoryData.folder.length();
		if (folderLength > 1 && categoryData.folder.charAt(folderLength - 1) == '/')
			categoryData.folder = categoryData.folder.substring(0, folderLength - 1);

		editText = (EditText) findViewById(R.id.dnode_referrer_editor);
		categoryData.referrer = editText.getText().toString();
		editText = (EditText) findViewById(R.id.dnode_user_editor);
		categoryData.user = editText.getText().toString();
		editText = (EditText) findViewById(R.id.dnode_password_editor);
		categoryData.password = editText.getText().toString();
		// connections
		editText = (EditText) findViewById(R.id.dnode_connections_editor);
		string = editText.getText().toString();
		if (string.length() > 0)
			categoryData.connections = Integer.parseInt(string);
		else
			categoryData.connections = 1;
		// retryLimit
//		editText = (EditText) findViewById(R.id.dnode_retry_editor);
//		string = editText.getText().toString();
//		if (string.length() > 0)
//			categoryData.retryLimit = Integer.parseInt(string);
//		else
//			categoryData.retryLimit = 10;
		// proxy port
		editText = (EditText) findViewById(R.id.dnode_proxy_port_editor);
		string = editText.getText().toString();
		if (string.length() > 0)
			categoryData.proxyPort = Integer.parseInt(string);
		else
			categoryData.proxyPort = 80;
		// proxy others
		editText = (EditText) findViewById(R.id.dnode_proxy_host_editor);
		categoryData.proxyHost = editText.getText().toString();
		editText = (EditText) findViewById(R.id.dnode_proxy_user_editor);
		categoryData.proxyUser = editText.getText().toString();
		editText = (EditText) findViewById(R.id.dnode_proxy_password_editor);
		categoryData.proxyPassword = editText.getText().toString();
		Spinner spinner = (Spinner) findViewById(R.id.dnode_proxy_type_spinner);
		categoryData.proxyType = spinner.getSelectedItemPosition();
	}

	// --------------------------------
	// folder chooser + permission

	private static final int FOLDER_CHOOSER_CODE = 42;
	private static final int FOLDER_REQUEST_CODE = 43;

	protected boolean isFolderWritable(String folder) {
		// for Android 5+
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			for (int index = 0;  index < app.folderWritable.length;  index++) {
				if (folder.startsWith(app.folderWritable[index]))
					return true;
			}

			List<UriPermission> list = getContentResolver().getPersistedUriPermissions();
			for (int i = 0; i < list.size(); i++){
				String folderFromUri = FileUtil.getFullPathFromTreeUri(list.get(i).getUri(), this);
				if (folder.startsWith(folderFromUri))
					if (list.get(i).isWritePermission())
						return true;
			}
			return false;
		}

		return true;
	}

	protected void runFolderChooser () {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
			intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
			intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
					Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
					Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, FOLDER_CHOOSER_CODE);
		}
	}

	protected void onFolderChooserResult (Uri  treeUri) {
		String folder = FileUtil.getFullPathFromTreeUri(treeUri, this);
		if (isFolderWritable(folder) == false) {
			onFolderRequestResult(treeUri);
			Toast.makeText(this, "Get permission for " + folder,
					Toast.LENGTH_SHORT).show();
		}

		EditText editText;
		editText = (EditText) findViewById(R.id.dnode_folder_editor);
		editText.setText(folder);
	}

	protected void runFolderRequest () {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Permission to access this folder is required. Please pick this folder to get write permission.")
				.setTitle("Permission required");

		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                    // intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
					intent.setFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
							Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
					startActivityForResult(intent, FOLDER_REQUEST_CODE);
				}
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void onFolderRequestResult (Uri  treeUri) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			grantUriPermission(getPackageName(), treeUri,
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
							Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			getContentResolver().takePersistableUriPermission(treeUri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION |
							Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
		if (resultCode == RESULT_CANCELED)
			return;
		Uri  treeUri = resultData.getData(); // you can't use Uri.fromFile() to get path

		DocumentFile  docFile = DocumentFile.fromTreeUri(this, treeUri);
		String name = docFile.getName();

		switch (requestCode) {
			case FOLDER_CHOOSER_CODE:
				onFolderChooserResult(treeUri);
				break;

			case FOLDER_REQUEST_CODE:
				onFolderRequestResult(treeUri);

				Toast.makeText(this, FileUtil.getFullPathFromTreeUri(treeUri,this),
						Toast.LENGTH_SHORT).show();
				break;

			default:
				break;
		}
	}

	// ------------------------------------------------------------------------
	// Ad
	View adView = null;

	public void initAd() {
		adView = app.adManager.add((LinearLayout) findViewById(R.id.linearAd), this, 0);
	}

}

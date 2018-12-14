package com.ugetdm.uget;

import android.app.ActionBar;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import com.ugetdm.uget.lib.Info;
import com.ugetdm.uget.lib.Node;
import com.ugetdm.uget.lib.Sequence;

public class SequenceActivity extends Activity {
    private MainApp  app = null;
    private String[] seqTypeName;
    private ArrayAdapter<String> seqAdapter;
    private Sequence sequence = new Sequence();    // Sequence batch
    private TextView preview;
    private boolean  autoComplete = false;
    //static
    private static int   nthCategoryReal = -1;
    private static int   startupMode = StartupMode.automatically;

    public static final class StartupMode {
        public static final int automatically = 0;
        public static final int manually = 1;
    }

    public static final class RangeType {
        public static final int none = 0;
        public static final int number = 1;
        public static final int character = 2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sequence);

        Bundle  bundle;
        bundle = getIntent().getExtras();
        if (nthCategoryReal == -1) {
            nthCategoryReal = bundle.getInt("nthCategory", 0) - 1;
            if (nthCategoryReal < 0)
                nthCategoryReal = 0;
        }

        app = (MainApp) getApplicationContext();
        preview = (TextView) findViewById(R.id.seq_preview);
        preview.setMovementMethod(new ScrollingMovementMethod());

        ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(R.string.menu_action_button_batch_sequence);

        initRangeSpinner();
        initEditor();

        setupHandler.postDelayed(setupRunnable, 500);
    }

    @Override
    protected void onDestroy() {
        nthCategoryReal = -1;  // reset this static value
        super.onDestroy();
    }

    // --------------------------------
    // OptionsMenu (in ActionBar)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_sequence, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View      anchorView;
        PopupMenu popupMenu;
        Menu      menu;

        switch(item.getItemId()) {
            case R.id.menu_category:
                anchorView = findViewById(R.id.menu_category);
                if (anchorView == null)
                    anchorView = findViewById(R.id.menu_action_ok);
                popupMenu = new PopupMenu(this, anchorView);
                menu = popupMenu.getMenu();
                long      pointer;

                int  nItem = Node.nChildren(app.core.nodeReal);
                for (int index = 0; index < nItem;  index++) {
                    pointer = Node.getNthChild(app.core.nodeReal, index);
                    // groupId,  itemId,  order,  string
                    menu.add(0, index, index, Info.getName(Node.data(pointer))).setChecked(index == nthCategoryReal);
                }
                popupMenu.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                nthCategoryReal = item.getItemId();
                                return true;
                            }
                        }
                );
                menu.setGroupCheckable(0, true, true);  // groupId,  checkable,  exclusive
                popupMenu.show();
                break;

            case R.id.menu_action_startup_mode:
                anchorView = findViewById(R.id.menu_action_startup_mode);
                if (anchorView == null)
                    anchorView = findViewById(R.id.menu_action_ok);
                popupMenu = new PopupMenu(this, anchorView);
                popupMenu.inflate(R.menu.download_mode);
                popupMenu.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.menu_download_start_auto:
                                        startupMode = StartupMode.automatically;
                                        break;

                                    case R.id.menu_download_start_manually:
                                        startupMode = StartupMode.manually;
                                        break;
                                }
                                return true;
                            }
                        }
                );
                menu  = popupMenu.getMenu();
                switch (startupMode) {
                    default:
                    case StartupMode.automatically:
                        menu.getItem(0).setChecked(true);
                        break;

                    case StartupMode.manually:
                        menu.getItem(1).setChecked(true);
                        break;
                }
                popupMenu.show();
                break;

            case R.id.menu_action_ok:
                EditText uriEditor = (EditText) findViewById(R.id.seq_uri_editor);
                long     cNodePointer = Node.getNthChild(app.core.nodeReal, nthCategoryReal);
                if (cNodePointer != 0) {
                    app.core.addDownloadSequence(sequence, uriEditor.getText().toString(), cNodePointer, startupMode);
                    // notify data changed
                    app.downloadAdapter.notifyDataSetChanged();
                    app.categoryAdapter.notifyDataSetChanged();
                    app.stateAdapter.notifyDataSetChanged();
                    if (app.mainActivity != null && app.mainActivity.categorySpinner != null) {
                        // update spinner displayed data
                        app.mainActivity.categorySpinner.setAdapter(null);
                        app.mainActivity.categorySpinner.setAdapter(app.categoryAdapter);
                    }
                }
                finish();
                break;

            default:
        }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------------
    // initialize

    public void initRangeSpinner() {
        seqTypeName = new String[]{getString(R.string.seq_type_none), getString(R.string.seq_type_number), getString(R.string.seq_type_char)};
        seqAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, seqTypeName);
        seqAdapter.setDropDownViewResource(R.layout.spinner_item);

        Spinner spinner;
        spinner = (Spinner) findViewById(R.id.seq_type1);
        spinner.setAdapter(seqAdapter);
        spinner.setSelection(RangeType.number);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                setRangeType(0, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinner = (Spinner) findViewById(R.id.seq_type2);
        spinner.setAdapter(seqAdapter);
        spinner.setSelection(RangeType.none);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                setRangeType(1, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinner = (Spinner) findViewById(R.id.seq_type3);
        spinner.setAdapter(seqAdapter);
        spinner.setSelection(RangeType.none);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                setRangeType(2, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    public void initEditor() {
        EditText editor;

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (autoComplete)
                    showPreview();
            }
        };

        editor = (EditText) findViewById(R.id.seq_uri_editor);
        editor.addTextChangedListener(textWatcher);

        editor = (EditText) findViewById(R.id.seq_digits1);
        editor.addTextChangedListener(textWatcher);
        editor = (EditText) findViewById(R.id.seq_from1);
        editor.addTextChangedListener(textWatcher);
        editor = (EditText) findViewById(R.id.seq_to1);
        editor.addTextChangedListener(textWatcher);

        editor = (EditText) findViewById(R.id.seq_digits2);
        editor.addTextChangedListener(textWatcher);
        editor = (EditText) findViewById(R.id.seq_from2);
        editor.addTextChangedListener(textWatcher);
        editor = (EditText) findViewById(R.id.seq_to2);
        editor.addTextChangedListener(textWatcher);

        editor = (EditText) findViewById(R.id.seq_digits3);
        editor.addTextChangedListener(textWatcher);
        editor = (EditText) findViewById(R.id.seq_from3);
        editor.addTextChangedListener(textWatcher);
        editor = (EditText) findViewById(R.id.seq_to3);
        editor.addTextChangedListener(textWatcher);
    }

    // ------------------------------------------------------------------------
    // Range

    public void setRangeType(int nthRange, int type) {
        View digitsLabel, charCase;
        EditText digits, from, to;

        switch (nthRange) {
            default:
            case 0:
                charCase = findViewById(R.id.seq_char_case1);
                digitsLabel = findViewById(R.id.seq_digits_label1);
                digits = (EditText) findViewById(R.id.seq_digits1);
                from = (EditText) findViewById(R.id.seq_from1);
                to = (EditText) findViewById(R.id.seq_to1);
                break;

            case 1:
                charCase = findViewById(R.id.seq_char_case2);
                digitsLabel = findViewById(R.id.seq_digits_label2);
                digits = (EditText) findViewById(R.id.seq_digits2);
                from = (EditText) findViewById(R.id.seq_from2);
                to = (EditText) findViewById(R.id.seq_to2);
                break;

            case 2:
                charCase = findViewById(R.id.seq_char_case3);
                digitsLabel = findViewById(R.id.seq_digits_label3);
                digits = (EditText) findViewById(R.id.seq_digits3);
                from = (EditText) findViewById(R.id.seq_from3);
                to = (EditText) findViewById(R.id.seq_to3);
                break;
        }

        switch (type) {
            case RangeType.none:
            default:
                digitsLabel.setVisibility(View.GONE);
                digits.setVisibility(View.GONE);
                charCase.setVisibility(View.INVISIBLE);
                from.setEnabled(false);
                to.setEnabled(false);
                break;

            case RangeType.number:
                digitsLabel.setVisibility(View.VISIBLE);
                digits.setVisibility(View.VISIBLE);
                charCase.setVisibility(View.GONE);
                from.setEnabled(true);
                from.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
                to.setEnabled(true);
                to.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
                if (from.getInputType() != InputType.TYPE_CLASS_NUMBER || (from.length() == 0 && to.length() == 0)) {
                    from.setInputType(InputType.TYPE_CLASS_NUMBER);
                    to.setInputType(InputType.TYPE_CLASS_NUMBER);
                    if (autoComplete) {
                        from.setText("0");
                        to.setText("10");
                    }
                }
                break;

            case RangeType.character:
                digitsLabel.setVisibility(View.GONE);
                digits.setVisibility(View.GONE);
                charCase.setVisibility(View.VISIBLE);
                from.setEnabled(true);
                from.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
                to.setEnabled(true);
                to.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
                if (from.getInputType() != InputType.TYPE_CLASS_TEXT || (from.length() == 0 && to.length() == 0)) {
                    from.setInputType(InputType.TYPE_CLASS_TEXT);
                    to.setInputType(InputType.TYPE_CLASS_TEXT);
                    if (autoComplete) {
                        from.setText("a");
                        to.setText("z");
                    }
                }
                break;
        }

        // show preview after type changed.
        if (autoComplete)
            showPreview();
    }

    public boolean addRange(int nthRange) {
        EditText fromEditor, toEditor, digitsEditor;
        Spinner typeSpinner;
        int from, to, digits, type;

        switch (nthRange) {
            default:
            case 0:
                typeSpinner = (Spinner) findViewById(R.id.seq_type1);
                fromEditor = (EditText) findViewById(R.id.seq_from1);
                toEditor = (EditText) findViewById(R.id.seq_to1);
                digitsEditor = (EditText) findViewById(R.id.seq_digits1);
                break;

            case 1:
                typeSpinner = (Spinner) findViewById(R.id.seq_type2);
                fromEditor = (EditText) findViewById(R.id.seq_from2);
                toEditor = (EditText) findViewById(R.id.seq_to2);
                digitsEditor = (EditText) findViewById(R.id.seq_digits2);
                break;

            case 2:
                typeSpinner = (Spinner) findViewById(R.id.seq_type3);
                fromEditor = (EditText) findViewById(R.id.seq_from3);
                toEditor = (EditText) findViewById(R.id.seq_to3);
                digitsEditor = (EditText) findViewById(R.id.seq_digits3);
                break;
        }

        if (fromEditor.length() == 0 || toEditor.length() == 0)
            return false;

        type = typeSpinner.getSelectedItemPosition();
        switch (type) {
            case RangeType.number:
                try {
                    from = Integer.parseInt(fromEditor.getText().toString());
                    to = Integer.parseInt(toEditor.getText().toString());
                    digits = Integer.parseInt(digitsEditor.getText().toString());
                } catch (Exception e) {
                    return false;
                }
                break;

            case RangeType.character:
                try {
                    from = fromEditor.getText().toString().charAt(0);
                    to = toEditor.getText().toString().charAt(0);
                    digits = 0;
                } catch (Exception e) {
                    return false;
                }
                break;

            case RangeType.none:
            default:
                return false;
        }

        sequence.add(from, to, digits);
        return true;
    }

    // ------------------------------------------------------------------------
    // Preview

    public void showPreview() {
        String uriString;
        int index;
        int count;

        EditText uriEditor = (EditText) findViewById(R.id.seq_uri_editor);

        uriString = uriEditor.getText().toString();
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if(uri.getScheme() == null) {
                preview.setText(getString(R.string.seq_msg_uri_not_valid));
                findViewById(R.id.menu_action_ok).setEnabled(false);
                return;
            }
        }

        for (count = 0, index = uriString.length() - 1; index >= 0; index--) {
            if (uriString.charAt(index) == '*') {
                count++;
            }
        }

        if (count == 0) {
            preview.setText(getString(R.string.seq_msg_no_wildcard));
            findViewById(R.id.menu_action_ok).setEnabled(false);
        }
        else {
            sequence.clear();
            addRange(0);
            addRange(1);
            addRange(2);

            String[] uriArray = sequence.getPreview(uriString);

            if (uriArray.length == 0) {
                preview.setText(getString(R.string.seq_msg_no_from_to));
                findViewById(R.id.menu_action_ok).setEnabled(false);
            }
            else {
                preview.setText("");
                for (index = 0; index < uriArray.length; index++)
                    preview.append(uriArray[index] + "\n");
                findViewById(R.id.menu_action_ok).setEnabled(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Handler
    private Handler setupHandler = new Handler();
    private Runnable setupRunnable = new Runnable() {
        @Override
        public void run() {
            Spinner  spinner;

            autoComplete = true;
            spinner = (Spinner) findViewById(R.id.seq_type1);
            setRangeType(0, spinner.getSelectedItemPosition());
            spinner = (Spinner) findViewById(R.id.seq_type2);
            setRangeType(1, spinner.getSelectedItemPosition());
            spinner = (Spinner) findViewById(R.id.seq_type3);
            setRangeType(2, spinner.getSelectedItemPosition());

            showPreview();
        }
    };
}

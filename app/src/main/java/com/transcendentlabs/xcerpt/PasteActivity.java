package com.transcendentlabs.xcerpt;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import com.transcendentlabs.xcerpt.BuildConfig;
import io.fabric.sdk.android.Fabric;


public class PasteActivity extends AppCompatActivity {

    private static final String TWITTER_KEY = BuildConfig.TWITTER_KEY;
    private static final String TWITTER_SECRET = BuildConfig.TWITTER_SECRET_KEY;


    private MenuItem pasteItem;
    private MenuItem nextItem;

    private EditText mEditText;
    private TextView mShareHint;
    private ClipboardManager clipboard;
    public static final String EXCERPT = "com.ericbai.xcerpt.excerpt";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Crashlytics(), new Twitter(authConfig));

        setContentView(R.layout.activity_paste);

        ActionBar bar = getSupportActionBar();
        if(bar != null){
            bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.material_blue_grey_800)));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(getResources().getColor(R.color.material_blue_grey_900));
            }
        }

        mEditText = (EditText) findViewById(R.id.edit_message);
        mShareHint = (TextView) findViewById(R.id.share_hint);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if(intentAction != null && intentAction.equals(Intent.ACTION_SEND)){
            mShareHint.setVisibility(View.GONE);
        }

        mEditText.setTypeface(Typeface.SERIF);

        if (!isNetworkAvailable(getApplicationContext())) {
            CharSequence text = getString(R.string.no_internet_error);
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.paste, menu);
        pasteItem =  menu.getItem(1);
        nextItem = menu.getItem(2);
        nextItem.setEnabled(false);
        if (!(clipboard.hasPrimaryClip())) {
            pasteItem.setEnabled(false);
        } else if (!(clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {
            // This disables the paste menu item, since the clipboard has data but it is not plain text
            pasteItem.setEnabled(true);
        } else {
            // This enables the paste menu item, since the clipboard contains plain text.
            pasteItem.setEnabled(true);
        }

        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if(intentAction != null && intentAction.equals(Intent.ACTION_SEND)){
            mEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT).trim());
        }

        if(mEditText.getText().length() > 0){
            nextItem.setEnabled(true);
        }

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean enableNext = mEditText.getText().length() > 0;
                nextItem.setEnabled(enableNext);
            }
        });

        return true;
    }

    public void delete() {
        mEditText.setText("");

        // show keyboard
        mEditText.requestFocus();
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    public void paste() {
        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        String pasteData = item.getText().toString();
        if(pasteData != null){
            mEditText.setText(pasteData.trim());
        }
        // hide keyboard
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public void next() {
        if (isNetworkAvailable(getApplicationContext())) {
            Intent intent = new Intent(this, CustomizeActivity.class);
            intent.setAction(Intent.ACTION_DEFAULT);
            String content = mEditText.getText().toString();
            intent.putExtra(EXCERPT, content);
            startActivity(intent);
        } else {
            CharSequence text = getString(R.string.no_internet_error);
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.delete) {
            delete();
            return true;
        }
        if(id == R.id.paste){
            paste();
            return true;
        }

        if(id == R.id.looks_good){
            next();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo() != null;
    }
}

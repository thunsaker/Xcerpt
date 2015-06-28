package ericbai.com.sharepiece;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class PasteActivity extends ActionBarActivity {
    private EditText mEditText;
    private ClipboardManager clipboard;
    private Button pasteButton;
    private Button nextButton;
    public static final String EXCERPT = "com.ericbai.xcerpt.excerpt";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paste);

        mEditText = (EditText) findViewById(R.id.edit_message);
        clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        pasteButton = (Button) findViewById(R.id.paste_button);
        nextButton = (Button) findViewById(R.id.next_button);

        nextButton.setEnabled(false);

        mEditText.setTypeface(Typeface.SERIF);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isNetworkAvailable(getApplicationContext())) {
                    boolean enableNext = mEditText.getText().length() > 0;
                    nextButton.setEnabled(enableNext);
                }
            }
        });

        if (!isNetworkAvailable(getApplicationContext())) {
            CharSequence text = "Error: Check your internet connection."; //TODO make const
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }

        if (!(clipboard.hasPrimaryClip())) {
            pasteButton.setEnabled(false);
        } else if (!(clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {
            // This disables the paste menu item, since the clipboard has data but it is not plain text
            pasteButton.setEnabled(true);
        } else {
            // This enables the paste menu item, since the clipboard contains plain text.
            pasteButton.setEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.paste, menu);
        return true;
    }

    public void delete(View view) {
        mEditText.setText("");

        // show keyboard
        mEditText.requestFocus();
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT);
    }

    public void paste(View view) {
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

    public void next(View view) {
        if (isNetworkAvailable(getApplicationContext())) {
            Intent intent = new Intent(this, CustomizeActivity.class);
            intent.setAction(Intent.ACTION_DEFAULT);
            String content = mEditText.getText().toString();
            intent.putExtra(EXCERPT, content);
            startActivity(intent);
        } else {
            CharSequence text = "Error: Check your internet connection."; //TODO make const
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo() != null;
    }
}

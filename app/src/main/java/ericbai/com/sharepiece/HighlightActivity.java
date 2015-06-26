package ericbai.com.sharepiece;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class HighlightActivity extends Activity {
    private String excerpt;
    private TextView mTextView;
    public static final float TEXT_SIZE = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_highlight);
        Intent intent = getIntent();
        excerpt = intent.getStringExtra(PasteActivity.EXCERPT);

        mTextView = (TextView) findViewById(R.id.article_text);
        mTextView.setTextColor(Color.BLACK);
        mTextView.setTextSize(TEXT_SIZE);
        mTextView.setTypeface(Typeface.SERIF);
        mTextView.setText(excerpt);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.highlight, menu);
        return true;
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
}

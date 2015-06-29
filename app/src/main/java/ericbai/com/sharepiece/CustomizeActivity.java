package ericbai.com.sharepiece;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class CustomizeActivity extends FragmentActivity {
    private String excerpt;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private TextView contentPreview;
    private LinearLayout backgroundView;
    private TextView titleView;
    private TextView websiteView;
    private LinearLayout wrapper;

    public String selectedUrl;
    public int selected_index = 0;

    public Article[] articles = new Article[3];

    private static final float TEXT_SIZE = 16;
    private static final int NUM_RESULTS = 3;
    private static final int MAX_HEIGHT = 1920;
    private static final String NO_SOURCE_FOUND = "No source found!"; //TODO put in strings.xml
    public static final String IMAGE = "IMAGE";
    public static final String URL = "URL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_2);

        backgroundView = (LinearLayout) findViewById(R.id.background);
        titleView = (TextView) findViewById(R.id.title);
        websiteView = (TextView) findViewById(R.id.website);
        contentPreview = (TextView) findViewById(R.id.content_preview);
        wrapper = (LinearLayout) findViewById(R.id.content);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(mPager);

        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if(intentAction.equals(Intent.ACTION_SEND)){
            excerpt = intent.getStringExtra(Intent.EXTRA_TEXT).trim();
        }else if(intentAction.equals(Intent.ACTION_DEFAULT)){
            excerpt = intent.getStringExtra(PasteActivity.EXCERPT);
        }

        contentPreview.setTypeface(Typeface.SERIF);

        contentPreview.setTextColor(Color.BLACK);
        contentPreview.setTextSize(TEXT_SIZE);
        contentPreview.setText(excerpt);

        SearchAsyncTask searchTask =
                new SearchAsyncTask(excerpt, NUM_RESULTS, new SearchAsyncTask.Callback() {
            @Override
            public void onComplete(Object o, Error error) {
                if(error != null){
                    Log.e("SearchAsyncTask", error.getMessage());
                    return;
                }
                BingSearchResults results = (BingSearchResults) o;
                try {
                    processResults(results.getResults());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
        searchTask.execute();
    }

    public void processResults(BingSearchResults.Result[] results) throws IOException {
        if(results == null || results.length == 0){
            titleView.setText(NO_SOURCE_FOUND);
            websiteView.setText(NO_SOURCE_FOUND);
            return;
        }

        for(int i = 0; i < results.length; i++){
            //TODO: get meta og:title from site's HTML instead of Bing's title
            String pageTitle = results[i].Title;

            String baseUrl = results[i].DisplayUrl;
            int backslashAt = baseUrl.indexOf('/');
            if(backslashAt > 0){
                baseUrl = baseUrl.substring(0, backslashAt);
            }

            articles[i] = new Article(pageTitle, baseUrl, results[i].Url);
        }
        titleView.setText(articles[0].title);
        websiteView.setText(articles[0].displayUrl);
        selectedUrl = articles[0].url;
        mPagerAdapter.notifyDataSetChanged();
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

    public void back(View view){
        finish();
    }

    public void share(View view) {
        Intent intent = new Intent(this, ShareActivity.class);

        intent.putExtra(URL, selectedUrl);

        Bitmap image = takeScreenShot();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        intent.putExtra(IMAGE, byteArray);
        startActivity(intent);
    }

    public class Article{
        String title;
        String displayUrl;
        String url;

        public Article(String title, String displayUrl, String url){
            this.title = title;
            this.displayUrl = displayUrl;
            this.url = url;
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {
        private final String[] TITLES = {
                getString(R.string.tab_highlight),
                getString(R.string.tab_colour),
                getString(R.string.tab_source)
        };

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public Fragment getItem(int position) {
            return ScreenSlidePageFragment.create(position);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
    }

    public void updateSource(int articleIndex){
        titleView.setText(articles[articleIndex].title);
        websiteView.setText(articles[articleIndex].displayUrl);
        selected_index = articleIndex;

    }

    public LinearLayout getBackgroundView() {
      return backgroundView;
    }

    private Bitmap takeScreenShot()
    {
        int totalHeight = wrapper.getChildAt(0).getHeight();
        int totalWidth = wrapper.getChildAt(0).getWidth();

        Bitmap b = getBitmapFromView(wrapper,totalHeight,totalWidth);

        return b;
    }
    public static Bitmap getBitmapFromView(View view, int totalHeight, int totalWidth) {

        int height = Math.min(MAX_HEIGHT, totalHeight);
        float percent = height / (float)totalHeight;

        Bitmap canvasBitmap = Bitmap.createBitmap(
                (int)(totalWidth*percent),
                (int)(totalHeight*percent),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(canvasBitmap);

        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);

        canvas.save();
        canvas.scale(percent, percent);
        view.draw(canvas);
        canvas.restore();

        return canvasBitmap;
    }
}

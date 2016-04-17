package com.transcendentlabs.xcerpt;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.transcendentlabs.xcerpt.views.MaxHeightScrollView;
import com.transcendentlabs.xcerpt.tasks.ParseHtmlAsyncTask;
import com.transcendentlabs.xcerpt.fragments.ScreenSlidePageFragment;
import com.transcendentlabs.xcerpt.tasks.SearchAsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

import static com.transcendentlabs.xcerpt.Util.DEFAULT_COLOUR;
import static com.transcendentlabs.xcerpt.Util.EXCERPT;
import static com.transcendentlabs.xcerpt.Util.getTextFromClipboard;

public class CustomizeActivity extends BaseActivity {

    private LinearLayout backgroundView;
    private PagerAdapter mPagerAdapter;
    private TextView titleView;
    private TextView websiteView;
    private TextView logoView;
    private PagerSlidingTabStrip tabs;
    private Button customSourceButton;
    private ClipboardManager clipboard;
    private TourGuide mTourGuideHandler;
    private String excerpt;
    public ArrayList<AsyncTask> tasks;

    // public parameters (are highly coupled in ScreenSlidePageFragment at the moment...)
    public TextView contentPreview;
    public MaxHeightScrollView scrollView;

    public String selectedUrl;
    public int selected_index = 0;
    public boolean no_results_or_error = false;

    public MenuItem nextItem;

    public Article[] articles = new Article[NUM_RESULTS];
    public int numResults;

    private static final int NUM_RESULTS = 3;
    private static final String SHOW_HINT_SETTING = "hint";
    private static final double MAX_SCROLL_VIEW_HEIGHT = 0.5; // as percentage of screen height

    public static final String IMAGE = "IMAGE";
    public static final String URL = "URL";
    public static final String COLOUR_SETTING = "colour";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        tasks = new ArrayList<>();

        backgroundView = (LinearLayout) findViewById(R.id.background);
        titleView = (TextView) findViewById(R.id.title);
        websiteView = (TextView) findViewById(R.id.website);

        logoView = (TextView) findViewById(R.id.logo);
        logoView.setTypeface(App.getLogoFont());

        customSourceButton = (Button) findViewById(R.id.custom_source_button);
        initCustomSourceButton(customSourceButton);

        performSearch();
    }

    @Override
    public void onResume() {
        super.onResume();
        String newExcerpt = getExcerptFromIntent();
        if(newExcerpt != null && !newExcerpt.equals(excerpt)){
            performSearch();
        }
    }

    private void performSearch() {
        titleView.setText(getString(R.string.loading));
        websiteView.setText(getString(R.string.loading));
        Arrays.fill(articles, null);
        initPager();
        excerpt = getExcerptFromIntent();
        final SharedPreferences settings = getPreferences(0);
        initContentPreview(settings, excerpt);
        showGuideIfNeeded(settings);
        executeSearchTask(excerpt);
    }

    private void initPager() {
        ViewPager mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(mPager);
        tabs.setTextColorResource(R.color.tw__solid_white);
        tabs.setIndicatorColor(Color.WHITE);
        tabs.setDividerColor(Color.TRANSPARENT);
    }

    private String getExcerptFromIntent() {
        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if(fromSharing(intentAction)){
            return intent.getStringExtra(Intent.EXTRA_TEXT).trim();
        }else if(fromXcerpt(intentAction)){
            return intent.getStringExtra(EXCERPT);
        }

        Log.e("CustomizeActivity", "Unexpected intent, excerpt String returned null");
        return null;
    }

    private boolean fromXcerpt(String intentAction) {
        return intentAction != null && intentAction.equals(Intent.ACTION_DEFAULT);
    }

    private boolean fromSharing(String intentAction) {
        return intentAction != null && intentAction.equals(Intent.ACTION_SEND);
    }

    private void initContentPreview(final SharedPreferences settings, String text) {
        initializeScrollView();

        contentPreview = (TextView) findViewById(R.id.content_preview);

        // set background colour to preferred colour
        int defaultColour = settings.getInt(COLOUR_SETTING, Color.parseColor(DEFAULT_COLOUR));
        setColour(defaultColour);

        formatText(text);

        initializeHighlightClearing();

        contentPreview.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
                int defaultColour = settings.getInt(
                        COLOUR_SETTING,
                        Color.parseColor(DEFAULT_COLOUR)
                );
                setColour(defaultColour);
            }

            public boolean onCreateActionMode(final ActionMode mode, Menu menu) {
                menu.clear();
                mode.getMenuInflater().inflate(R.menu.highlight, menu);
                boolean showHint = settings.getBoolean(SHOW_HINT_SETTING, true);
                if (showHint) {
                    mTourGuideHandler.cleanUp();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(SHOW_HINT_SETTING, false);
                    editor.commit();
                }
                return true;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                switch (item.getItemId()) {

                    case R.id.done:
                        share();
                        return true;
                }
                return false;
            }
        });
    }

    private void initializeHighlightClearing() {
        final GestureDetector gestureDetector = new GestureDetector(
                this,
                new GestureDetector.SimpleOnGestureListener(){
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        contentPreview.clearFocus();
                        return true;
                    }
            });
        contentPreview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    private void formatText(String text) {
        final float TEXT_SIZE = 16;
        contentPreview.setTypeface(Typeface.SERIF);
        contentPreview.setTextColor(Color.BLACK);
        contentPreview.setTextSize(TEXT_SIZE);
        contentPreview.setText(text);
    }

    private void initializeScrollView() {
        // set scroll view (content preview's parent) to max height
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;
        scrollView = (MaxHeightScrollView) findViewById(R.id.preview_scroll);
        scrollView.setMaxHeight((int) (MAX_SCROLL_VIEW_HEIGHT * height));
    }

    private void showGuideIfNeeded(final SharedPreferences settings) {
        boolean showHint = settings.getBoolean(SHOW_HINT_SETTING, true);

        if(showHint) {
            TextView contentPreview = (TextView) findViewById(R.id.content_preview);
            String description = getString(R.string.highlight_hint_marshmallow);
            mTourGuideHandler = TourGuide.init(this).with(TourGuide.Technique.Click)
                    .setPointer(new Pointer().setGravity(Gravity.TOP))
                    .setToolTip(new ToolTip()
                            .setTitle(getString(R.string.highlight_hint_title))
                            .setDescription(description)
                            .setGravity(Gravity.TOP))
                    .setOverlay(new Overlay())
                    .playOn(contentPreview);
        }
    }

    private void executeSearchTask(String textToSearch) {
        SearchAsyncTask searchTask = new SearchAsyncTask(textToSearch, NUM_RESULTS, new SearchAsyncTask.Callback() {
            @Override
            public void onComplete(Object o, Error error) {
                if (error != null) {
                    no_results_or_error = true;
                    mPagerAdapter.notifyDataSetChanged();
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
        tasks.add(searchTask);
        searchTask.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for(AsyncTask task : tasks){
            task.cancel(true);
        }
    }

    public void processResults(final BingSearchResults.Result[] results) throws IOException {
        if(results == null || results.length == 0){
            showNoSourceFoundError();
            return;
        }

        numResults = results.length;

        for(int i = 0; i < results.length; i++){
            final int finalI = i;
            ParseHtmlAsyncTask titleTask = new ParseHtmlAsyncTask(results[i].url,
                    new ParseHtmlAsyncTask.Callback() {
                        @Override
                        public void onComplete(Object o, Error error) {
                            setArticle((String) o, error, finalI, results);
                        }
                    });
            tasks.add(titleTask);
            titleTask.execute();
        }
    }

    private void setArticle(String pageTitle, Error error, int articleIndex, BingSearchResults.Result[] results) {
        if (error != null) {
            // fall back on search result data
            articles[articleIndex] = createArticle(
                    results[articleIndex].title,
                    results[articleIndex].displayUrl,
                    results[articleIndex].url
            );
            // TODO crashlytics log
        }else {
            if (pageTitle.length() == 0) {
                // fall back to search result title
                pageTitle = results[articleIndex].title;
            }

            articles[articleIndex] = createArticle(
                    pageTitle,
                    results[articleIndex].displayUrl,
                    results[articleIndex].url
            );
        }

        if (articleIndex == 0) {
            showAtLeastOneSourceFound();
        }
        mPagerAdapter.notifyDataSetChanged();
    }

    private void showAtLeastOneSourceFound() {
        setTitleText(articles[0].getTitle());
        websiteView.setText(articles[0].getDisplayUrl());
        selectedUrl = articles[0].getUrl();
        nextItem.setEnabled(true);
    }

    private void showNoSourceFoundError() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                final ObjectAnimator animScrollToTop =
                        ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.getBottom());
                animScrollToTop.setDuration(500);
                animScrollToTop.start();

                setTitleText(getString(R.string.no_source_found));
                websiteView.setVisibility(View.GONE);
                customSourceButton.setVisibility(View.VISIBLE);

                no_results_or_error = true;
                mPagerAdapter.notifyDataSetChanged();
            }
        });
    }

    private Article createArticle(String title, String displayUrl, String url){
        String baseUrl = displayUrl;
        String https = "https://";
        if (baseUrl.startsWith(https)) {
            baseUrl = baseUrl.substring(https.length());
        }
        int backslashAt = baseUrl.indexOf('/');
        if (backslashAt > 0) {
            baseUrl = baseUrl.substring(0, backslashAt);
        }

        return new Article(title, baseUrl, url);
    }

    private void setTitleText(String title){
        if (title == null) return;
        if(title.isEmpty()){
            titleView.setVisibility(View.GONE);
        }else{
            titleView.setText(title);
            titleView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.highlight, menu);
        nextItem = menu.getItem(0);
        nextItem.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.done) {
            share();
            return true;
        }
        if(id == R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void share(){
        if(selectedUrl == null){
            Toast.makeText(this, "You must select a source first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        deleteScreenshotIfNeeded();

        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(URL, selectedUrl);

        Bitmap image = getBitmapFromView(
                backgroundView,
                backgroundView.getHeight(),
                backgroundView.getWidth()
        );
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        intent.putExtra(IMAGE, byteArray);
        startActivity(intent);
    }

    private void deleteScreenshotIfNeeded() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPref.getBoolean(SettingsActivity.KEY_DELETE_SCREENSHOT, false)) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                return;
            }
            String uriString = extras.getString(InputActivity.IMAGE);
            if(uriString == null || uriString.isEmpty()) {
                return;
            }
            Uri uri = Uri.parse(uriString);
            File file = new File(uri.getPath());
            if(file.exists()) {
                if (file.delete()) {
                    MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });
                }
            }
        }
    }

    public void updateSource(final int articleIndex){
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                final ObjectAnimator animScrollToTop =
                        ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.getBottom());
                animScrollToTop.setDuration(500);
                animScrollToTop.start();
                setTitleText(articles[articleIndex].getTitle());
                websiteView.setText(articles[articleIndex].getDisplayUrl());
                selectedUrl = articles[articleIndex].getUrl();
                selected_index = articleIndex;
            }
        });
    }

    public void updateSource(final Article article){
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                final ObjectAnimator animScrollToTop =
                        ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.getBottom());
                animScrollToTop.setDuration(500);
                animScrollToTop.start();

                setTitleText(article.getTitle());
                websiteView.setVisibility(View.VISIBLE);
                customSourceButton.setVisibility(View.GONE);
                websiteView.setText(article.getDisplayUrl());
                selectedUrl = article.getUrl();
            }
        });

    }

    public void setColour(int colour){
        backgroundView.setBackgroundColor(colour);

        // set highlight colour to a lighter version of colour
        String hexColour = String.format("#%06X", (0xFFFFFF & colour));
        String highlightColour = "#40" + hexColour.substring(1);
        contentPreview.setHighlightColor(Color.parseColor(highlightColour));

        tabs.setBackgroundColor(colour);
        tabs.setIndicatorHeight(15);

        ActionBar bar = getSupportActionBar();
        if(bar != null){
            bar.setElevation(0);
            bar.setBackgroundDrawable(new ColorDrawable(colour));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                Bitmap icon =
                        BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

                ActivityManager.TaskDescription taskDescription =
                        new ActivityManager.TaskDescription(
                                getString(R.string.app_name),
                                icon,
                                colour);
                setTaskDescription(taskDescription);

                float[] hsv = new float[3];
                int darkerColour = colour;
                Color.colorToHSV(darkerColour, hsv);
                hsv[2] *= 0.8f; // value component
                darkerColour = Color.HSVToColor(hsv); // make colour darker
                window.setStatusBarColor(darkerColour);
            }
        }
    }

    public static Bitmap getBitmapFromView(View view, int totalHeight, int totalWidth) {
        final int MAX_IMAGE_HEIGHT = 1920;

        int height = Math.min(MAX_IMAGE_HEIGHT, totalHeight);
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

    public void initCustomSourceButton(Button customSourceButton) {
        customSourceButton.setTextColor(getResources().getColor(R.color.black_trans_65));
        customSourceButton.setBackgroundResource(R.drawable.customize_button_inline);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.gravity = Gravity.CENTER_HORIZONTAL;
        customSourceButton.setLayoutParams(buttonParams);
        customSourceButton.setTextSize(12);

        customSourceButton.setText("Use URL from clipboard");
        customSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String pasteData = getTextFromClipboard(CustomizeActivity.this, clipboard);
                if(pasteData != null){
                    getSourceHtml(pasteData);
                }else{
                    Toast.makeText(CustomizeActivity.this, getString(R.string.empty_clipboard_toast),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getSourceHtml(final String url) {
        // check if text is URL
        ParseHtmlAsyncTask titleTask =
                new ParseHtmlAsyncTask(url, new ParseHtmlAsyncTask.Callback(){
                    @Override
                    public void onComplete(Object o, Error error) {
                        if(error != null){
                            Toast.makeText(CustomizeActivity.this, "The clipboard text is not a valid URL.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String title = (String) o;

                        String baseUrl = App.getDisplayUrl(url);

                        Article customArticle = new Article(title, baseUrl, url);
                        updateSource(customArticle);
                        nextItem.setEnabled(true);
                    }
                });
        tasks.add(titleTask);
        titleTask.execute();
    }

    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {
        private final String[] TITLES = {
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
}
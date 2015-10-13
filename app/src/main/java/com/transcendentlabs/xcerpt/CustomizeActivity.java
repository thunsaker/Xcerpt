package com.transcendentlabs.xcerpt;

import android.animation.ObjectAnimator;
import android.app.ActivityManager;
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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import tourguide.tourguide.Overlay;
import tourguide.tourguide.Pointer;
import tourguide.tourguide.ToolTip;
import tourguide.tourguide.TourGuide;

import static com.transcendentlabs.xcerpt.Util.DEFAULT_COLOUR;
import static com.transcendentlabs.xcerpt.Util.EXCERPT;

public class CustomizeActivity extends AppCompatActivity {

    private LinearLayout backgroundView;
    private PagerAdapter mPagerAdapter;
    private TextView titleView;
    private TextView websiteView;
    private PagerSlidingTabStrip tabs;
    private TourGuide mTourGuideHandler;
    private String excerpt;

    // public parameters (are highly coupled in ScreenSlidePageFragment at the moment...)
    public TextView contentPreview;
    public MaxHeightScrollView scrollView;

    public String selectedUrl;
    public int selected_index = 0;
    public boolean no_results_or_error = false;

    public MenuItem nextItem;
    public MenuItem actionModeNextItem = null;

    public Article[] articles = new Article[NUM_RESULTS];
    public int numResults;

    private static final int NUM_RESULTS = 3;
    private static final String SHOW_HINT_SETTING = "hint";
    private static final double MAX_SCROLL_VIEW_HEIGHT = 0.5; // as percentage of screen height

    public static final String IMAGE = "IMAGE";
    public static final String URL = "URL";
    public static final String COLOUR_SETTING = "colour";
    public boolean actionModeOpen = false;

    volatile boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize);
        running = true;

        backgroundView = (LinearLayout) findViewById(R.id.background);
        titleView = (TextView) findViewById(R.id.title);
        websiteView = (TextView) findViewById(R.id.website);

        initPager();

        excerpt = getExcerptFromIntent();

        final SharedPreferences settings = getPreferences(0);
        initContentPreview(settings, excerpt);
        showGuide(settings);

        executeSearchTask(excerpt);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        String newExcerpt = getExcerptFromIntent();
        if(newExcerpt != null && !newExcerpt.equals(excerpt)){
            titleView.setText(getString(R.string.loading));
            websiteView.setText(getString(R.string.loading));
            Arrays.fill(articles, null);
            initPager();
            excerpt = newExcerpt;
            final SharedPreferences settings = getPreferences(0);
            initContentPreview(settings, excerpt);
            showGuide(settings);
            executeSearchTask(excerpt);
        }
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

        if(intentAction != null && intentAction.equals(Intent.ACTION_SEND)){ // from sharing
            return intent.getStringExtra(Intent.EXTRA_TEXT).trim();
        }else if(intentAction != null && intentAction.equals(Intent.ACTION_DEFAULT)){ // from Xcerpt
            return intent.getStringExtra(EXCERPT);
        }

        Log.e("CustomizeActivity", "Unexpected intent, excerpt String returned null");
        return null;
    }

    private void initContentPreview(final SharedPreferences settings, String text) {
        final float TEXT_SIZE = 16;

        // set scroll view (content preview's parent) to max height
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;
        scrollView = (MaxHeightScrollView) findViewById(R.id.preview_scroll);
        scrollView.setMaxHeight((int) (MAX_SCROLL_VIEW_HEIGHT * height));

        contentPreview = (TextView) findViewById(R.id.content_preview);

        // set background colour to preferred colour
        int defaultColour = settings.getInt(COLOUR_SETTING, Color.parseColor(DEFAULT_COLOUR));
        setColour(defaultColour);

        // format text
        contentPreview.setTypeface(Typeface.SERIF);
        contentPreview.setTextColor(Color.BLACK);
        contentPreview.setTextSize(TEXT_SIZE);
        contentPreview.setText(text);

        // highlighting
        contentPreview.setKeyListener(null);
        contentPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contentPreview.performLongClick();

                boolean showHint = settings.getBoolean(SHOW_HINT_SETTING, true);
                if(showHint) {
                    mTourGuideHandler.cleanUp();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(SHOW_HINT_SETTING, false);
                    editor.commit();
                }
            }
        });
        contentPreview.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
                contentPreview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        contentPreview.performLongClick();
                    }
                });
                int defaultColour = settings.getInt(
                        COLOUR_SETTING,
                        Color.parseColor(DEFAULT_COLOUR)
                );
                actionModeOpen = false;
                setColour(defaultColour);
            }

            public boolean onCreateActionMode(final ActionMode mode, Menu menu) {

                mode.getMenuInflater().inflate(R.menu.highlight, menu);
                menu.removeItem(android.R.id.copy);
                menu.removeItem(android.R.id.selectAll);

                actionModeNextItem = menu.getItem(0);
                actionModeNextItem.setEnabled(nextItem.isEnabled());

                contentPreview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mode.finish();
                    }
                });
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(getResources().getColor(R.color.material_blue_grey_900));
                }

                actionModeOpen = true;
                return true;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                switch(item.getItemId()) {

                    case R.id.done:
                        share();
                        return true;
                }
                return false;
            }
        });
    }

    private void showGuide(final SharedPreferences settings) {
        boolean showHint = settings.getBoolean(SHOW_HINT_SETTING, true);

        if(showHint) {
            TextView contentPreview = (TextView) findViewById(R.id.content_preview);
            mTourGuideHandler = TourGuide.init(this).with(TourGuide.Technique.Click)
                    .setPointer(new Pointer().setGravity(Gravity.TOP))
                    .setToolTip(new ToolTip()
                            .setTitle(getString(R.string.highlight_hint_title))
                            .setDescription(getString(R.string.highlight_hint))
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
                    Log.e("SearchAsyncTask", error.getMessage());
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
        searchTask.execute();
    }

    @Override
    protected void onDestroy() {
        running = false;
        super.onDestroy();
    }

    public void processResults(final BingSearchResults.Result[] results) throws IOException {
        if(results == null || results.length == 0){
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    final ObjectAnimator animScrollToTop =
                            ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.getBottom());
                    animScrollToTop.setDuration(500);
                    animScrollToTop.start();

                    setTitleText(getString(R.string.no_source_found));
                    websiteView.setText(R.string.no_source_instructions);

                    no_results_or_error = true;
                    mPagerAdapter.notifyDataSetChanged();
                }
            });
            return;
        }

        numResults = results.length;

        for(int i = 0; i < results.length; i++){
            final int finalI = i;
            ParseHtmlAsyncTask titleTask = new ParseHtmlAsyncTask(results[i].Url,
                    new ParseHtmlAsyncTask.Callback() {
                        @Override
                        public void onComplete(Object o, Error error) {
                            if (error != null) {
                                // fall back on search result data
                                articles[finalI] = createArticle(
                                        results[finalI].Title,
                                        results[finalI].DisplayUrl,
                                        results[finalI].Url
                                );
                                mPagerAdapter.notifyDataSetChanged();
                                if (error.getMessage() != null) {
                                    Log.e("SearchAsyncTask", error.getMessage());
                                    return;
                                } else {
                                    Log.e("SearchAsyncTask", "Unknown error");
                                    return;
                                }
                            }
                            String pageTitle = (String) o;
                            if (pageTitle.length() == 0) {
                                // fall back to search result title
                                pageTitle = results[finalI].Title;
                            }

                            articles[finalI] = createArticle(
                                    pageTitle,
                                    results[finalI].DisplayUrl,
                                    results[finalI].Url
                            );

                            if (finalI == 0) {
                                setTitleText(articles[0].title);
                                websiteView.setText(articles[0].displayUrl);
                                selectedUrl = articles[0].url;
                                nextItem.setEnabled(true);
                                if(actionModeOpen) actionModeNextItem.setEnabled(true);
                            }
                            if (running) {
                                mPagerAdapter.notifyDataSetChanged();
                            }
                        }
                    });
            titleTask.execute();
        }
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

    public void updateSource(final int articleIndex){
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                final ObjectAnimator animScrollToTop =
                        ObjectAnimator.ofInt(scrollView, "scrollY", scrollView.getBottom());
                animScrollToTop.setDuration(500);
                animScrollToTop.start();
                setTitleText(articles[articleIndex].title);
                websiteView.setText(articles[articleIndex].displayUrl);
                selectedUrl = articles[articleIndex].url;
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

                setTitleText(article.title);
                websiteView.setText(article.displayUrl);
                selectedUrl = article.url;
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
            if (!actionModeOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                Bitmap icon =
                        BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_white);

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
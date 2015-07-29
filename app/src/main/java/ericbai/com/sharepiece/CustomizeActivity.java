package ericbai.com.sharepiece;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.astuetz.PagerSlidingTabStrip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CustomizeActivity extends AppCompatActivity {

    private String excerpt;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    public TextView contentPreview;
    private LinearLayout backgroundView;
    private TextView titleView;
    private TextView websiteView;
    public ScrollView scrollView;

    public String selectedUrl;
    public int selected_index = 0;
    public boolean no_results = false;

    public MenuItem nextItem;
    public MenuItem actionModeNextItem = null;

    public Article[] articles = new Article[3];

    private static final float TEXT_SIZE = 16;
    private static final int NUM_RESULTS = 3;
    private static final int MAX_HEIGHT = 1920;
    public static final String IMAGE = "IMAGE";
    public static final String URL = "URL";
    public static final String COLOUR_SETTING = "colour";
    private static final String SHOW_HINT_SETTING = "hint";
    public boolean actionModeOpen = false;

    volatile boolean running;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_2);
        running = true;

        backgroundView = (LinearLayout) findViewById(R.id.background);
        titleView = (TextView) findViewById(R.id.title);
        websiteView = (TextView) findViewById(R.id.website);
        contentPreview = (TextView) findViewById(R.id.content_preview);
        scrollView = (ScrollView) findViewById(R.id.preview_scroll);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        final PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setViewPager(mPager);
        tabs.setTextColorResource(R.color.tw__solid_white);
        tabs.setIndicatorColor(Color.WHITE);
        tabs.setDividerColor(Color.TRANSPARENT);

        Intent intent = getIntent();
        String intentAction = intent.getAction();

        if(intentAction != null && intentAction.equals(Intent.ACTION_SEND)){
            excerpt = intent.getStringExtra(Intent.EXTRA_TEXT).trim();
        }else if(intentAction != null && intentAction.equals(Intent.ACTION_DEFAULT)){
            excerpt = intent.getStringExtra(PasteActivity.EXCERPT);
        }


        final SharedPreferences settings = getPreferences(0);
        int defaultColour = settings.getInt(COLOUR_SETTING, Color.parseColor("#9C27B0"));
        setColour(defaultColour);

        contentPreview.setTypeface(Typeface.SERIF);
        contentPreview.setTextColor(Color.BLACK);
        contentPreview.setTextSize(TEXT_SIZE);
        contentPreview.setText(excerpt);
        contentPreview.setKeyListener(null);
        contentPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    contentPreview.performLongClick();
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
                int defaultColour = settings.getInt(COLOUR_SETTING, Color.parseColor("#9C27B0"));
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

        boolean showHint = settings.getBoolean(SHOW_HINT_SETTING, true);

        if(showHint) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(getString(R.string.highlight_hint));
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            builder.setNegativeButton("Don't show again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(SHOW_HINT_SETTING, false);
                    editor.commit();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        SearchAsyncTask searchTask = new SearchAsyncTask(excerpt, NUM_RESULTS, new SearchAsyncTask.Callback() {
            @Override
            public void onComplete(Object o, Error error) {
                if (error != null) {
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

    @Override
    protected void onDestroy() {
        running = false;
        super.onDestroy();
    }

    public void processResults(final BingSearchResults.Result[] results) throws IOException {
        if(results == null || results.length == 0){
            titleView.setText(getString(R.string.no_source_found));
            websiteView.setText(R.string.no_source_instructions);
            no_results = true;
            mPagerAdapter.notifyDataSetChanged();
            return;
        }

        for(int i = 0; i < results.length; i++){
            final int finalI = i;
            ParseHtmlAsyncTask titleTask = new ParseHtmlAsyncTask(results[i].Url,
                    new ParseHtmlAsyncTask.Callback() {
                        @Override
                        public void onComplete(Object o, Error error) {
                            if (error != null) {
                                if (error.getMessage() != null) {
                                    Log.e("SearchAsyncTask", error.getMessage());
                                    //TODO: show user something went wrong
                                    return;
                                } else {
                                    Log.e("SearchAsyncTask", "Unknown error");
                                    return;
                                }
                            }
                            String pageTitle = (String) o;
                            if (pageTitle.length() == 0) {
                                //TODO handle when JSoup thinks there's no title?
                            }
                            String baseUrl = results[finalI].DisplayUrl;
                            String https = "https://";
                            if (baseUrl.startsWith(https)) {
                                baseUrl = baseUrl.substring(https.length());
                            }
                            int backslashAt = baseUrl.indexOf('/');
                            if (backslashAt > 0) {
                                baseUrl = baseUrl.substring(0, backslashAt);
                            }

                            articles[finalI] = new Article(pageTitle, baseUrl, results[finalI].Url);

                            if (finalI == 0) {
                                titleView.setText(articles[0].title);
                                websiteView.setText(articles[0].displayUrl);
                                selectedUrl = articles[0].url;
                                nextItem.setEnabled(true);
                            }
                            if (running) {
                                mPagerAdapter.notifyDataSetChanged();
                            }
                        }
                    });
            titleTask.execute();
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

    private void share(){
        Intent intent = new Intent(this, ShareActivity.class);

        intent.putExtra(URL, selectedUrl);

        Bitmap image = takeScreenShot();
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

                // scrollView.scrollTo(0, scrollView.getBottom());
                titleView.setText(articles[articleIndex].title);
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
                // scrollView.scrollTo(0, scrollView.getBottom());

                titleView.setText(article.title);
                websiteView.setText(article.displayUrl);
                selectedUrl = article.url;
            }
        });

    }

    public void setColour(int colour){
        backgroundView.setBackgroundColor(colour);
        String hexColour = String.format("#%06X", (0xFFFFFF & colour));
        String highlightColour = "#40" + hexColour.substring(1);
        contentPreview.setHighlightColor(Color.parseColor(highlightColour));

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setBackgroundColor(colour);
        tabs.setIndicatorHeight(15);

        ActionBar bar = getSupportActionBar();
        if(bar != null){
            bar.setElevation(0);
            bar.setBackgroundDrawable(new ColorDrawable(colour));
            if (!actionModeOpen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                float[] hsv = new float[3];
                int darkerColour = colour;
                Color.colorToHSV(darkerColour, hsv);
                hsv[2] *= 0.8f; // value component
                darkerColour = Color.HSVToColor(hsv);
                window.setStatusBarColor(darkerColour);
            }
        }
    }

    private Bitmap takeScreenShot()
    {
        int totalHeight = backgroundView.getHeight();
        int totalWidth = backgroundView.getWidth();

        Bitmap b = getBitmapFromView(backgroundView,totalHeight,totalWidth);

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
package com.transcendentlabs.xcerpt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.transcendentlabs.xcerpt.views.AspectRatioImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.widget.ImageView.ScaleType.CENTER_CROP;
import static com.transcendentlabs.xcerpt.Util.*;


public class InputActivity extends BaseActivity {

    public static final String IMAGE = "com.transcendentlabs.xcerpt.image";

    /** Resource to use for data file downloads. */
    public static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

    private GridView gv;
    private GridViewAdapter gvAdapter;
    private TextView selectScreenshot;
    private TextView noScreenshot;
    private Button pasteButton;

    private ClipboardManager clipboard;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        initializeViews();

        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        setActionBarElevationOff();
        setCustomActionBarLayout();

        deleteTempFolder();
    }

    private void deleteTempFolder() {
        String root =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File dir = new File(root + File.separator + "Xcerpt" + File.separator + "tmp");
        deleteFolder(dir);
    }

    private void setCustomActionBarLayout() {
        ActionBar bar = getSupportActionBar();
        Window window = getWindow();
        setActionBarColour(bar, window, this);
        if(bar != null) {
            bar.setDisplayShowCustomEnabled(true);
            bar.setDisplayShowTitleEnabled(false);

            LayoutInflater inflator = LayoutInflater.from(this);
            View v = inflator.inflate(R.layout.custom_action_bar, null);
            ((TextView)v.findViewById(R.id.title)).setTypeface(App.getLogoFont());
            bar.setCustomView(v);
        }
    }

    private void initializeViews() {
        selectScreenshot = (TextView) findViewById(R.id.select_screenshot);
        noScreenshot = (TextView) findViewById(R.id.no_screenshot);
        pasteButton = (Button) findViewById(R.id.paste_button);
        gv = (GridView) findViewById(R.id.image_grid);
    }

    @Override
    protected void onResume(){
        super.onResume();
        initOcrIfNecessary(this);
        updateScreenshotGallery();

        updatePasteButton();
    }

    private void updatePasteButton() {
        if(clipboardEmpty()) {
            disablePasteButton();
        } else {
            enablePasteButton();
        }
    }

    private void enablePasteButton() {
        pasteButton.setEnabled(true);
        pasteButton.setText(getString(R.string.paste_instructions));
        pasteButton.setTextColor(Color.WHITE);
    }

    private void disablePasteButton() {
        pasteButton.setEnabled(false);
        pasteButton.setText(getString(R.string.clipboard_empty));
        pasteButton.setTextColor(getResources().getColor(R.color.white_trans_65));
    }

    private boolean clipboardEmpty() {
        ClipData primaryClip = clipboard.getPrimaryClip();
        return primaryClip == null
                || primaryClip.getItemAt(0) == null
                || primaryClip.getItemAt(0).getText() == null
                || primaryClip.getItemAt(0).getText().toString().trim().isEmpty();
    }

    private void updateScreenshotGallery() {
        gvAdapter = new GridViewAdapter(this);
        gv.setAdapter(gvAdapter);

        if(gvAdapter.isEmpty()){
            noScreenshot.setVisibility(View.VISIBLE);
            selectScreenshot.setVisibility(View.GONE);
        }else{
            noScreenshot.setVisibility(View.GONE);
            selectScreenshot.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Picasso.with(this).cancelTag(this);
    }

    public static List<String> getCameraImages(Context context) {
        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED};

        final Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                MediaStore.Images.Media.DATA + " like ? ",
                new String[] {"%/Screenshots/%"},
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );
        if(cursor == null) {
            return new ArrayList<>();
        }
        ArrayList<String> result = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do {
                final String data = cursor.getString(dataColumn);
                result.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    public void pasteClipboard(View view) {
        String pasteData = getTextFromClipboard(this, clipboard);

        String excerpt = "";
        if(pasteData != null){
            excerpt = pasteData.trim();
        }else{
            return;
        }

        if(excerpt.length() <= 0){
            showEmptyClipboardError();
            return;
        }

        if (App.getInstance().isNetworkAvailable()) {
            pushToCustomizePage(excerpt);
        } else {
            showNoNetworkError();
        }
    }

    private void showEmptyClipboardError() {
        Toast.makeText(getApplicationContext(),
                getString(R.string.empty_clipboard_toast),
                Toast.LENGTH_SHORT)
                .show();
    }

    private void showNoNetworkError() {
        CharSequence text = getString(R.string.no_internet_error);
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void pushToCustomizePage(String excerpt) {
        Intent intent = new Intent(this, CustomizeActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        intent.putExtra(EXCERPT, excerpt);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_input, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            pushToSettingsPage();
        }
        return super.onOptionsItemSelected(item);
    }

    private void pushToSettingsPage() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        startActivity(intent);
    }

    void deleteFolder(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteFolder(child);

        fileOrDirectory.delete();
    }

    class GridViewAdapter extends BaseAdapter {
        private final Context context;
        List<String> urls;

        public GridViewAdapter(Context context) {
            this.context = context;
            urls = getCameraImages(context);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            AspectRatioImageView view = (AspectRatioImageView) convertView;
            if (view == null) {
                view = getScreenshotThumbnailView();
            }

            // Get the image URL for the current position.
            final Uri source = getItem(position);

            Picasso.with(context)
                    .load(source)
                    .placeholder(R.color.tw__light_gray)
                    .error(R.color.material_blue_grey_800)
                    .fit()
                    .centerCrop()
                    .tag(context)
                    .into(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openScreenshot(source);
                }
            });

            return view;
        }

        private AspectRatioImageView getScreenshotThumbnailView() {
            AspectRatioImageView view;Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            float ratio = ((float) size.y) / size.x;

            view = new AspectRatioImageView(context, ratio);
            view.setScaleType(CENTER_CROP);
            return view;
        }

        private void openScreenshot(Uri source) {
            if(source.toString() == null || source.toString().isEmpty()){
                // TODO show error
                return;
            }

            try {
                Bitmap img = MediaStore.Images.Media.getBitmap(InputActivity.this.getContentResolver(), source);
                if(img == null) {
                    // TODO show error
                    return;
                }
            } catch (IOException e) {
                // TODO show error
                return;
            }

            pushToCropPage(source);
        }

        private void pushToCropPage(Uri source) {
            Intent intent = new Intent(context, CropActivity.class);
            intent.putExtra(IMAGE, source.toString());
            startActivity(intent);
        }

        @Override public int getCount() {
            return urls.size();
        }

        @Override public Uri getItem(int position) {
            return Uri.fromFile(new File(urls.get(position)));
        }

        @Override public long getItemId(int position) {
            return position;
        }
    }
}
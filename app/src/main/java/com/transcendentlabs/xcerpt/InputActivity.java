package com.transcendentlabs.xcerpt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.widget.ImageView.ScaleType.CENTER_CROP;
import static com.transcendentlabs.xcerpt.Util.*;


public class InputActivity extends AppCompatActivity{

    public static final String IMAGE = "com.transcendentlabs.xcerpt.image";

    /** Resource to use for data file downloads. */
    static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

    private GridView gv;
    private GridViewAdapter gvAdapter;
    private TextView selectScreenshot;
    private TextView noScreenshot;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        selectScreenshot = (TextView) findViewById(R.id.select_screenshot);
        noScreenshot = (TextView) findViewById(R.id.no_screenshot);

        gv = (GridView) findViewById(R.id.image_grid);
        gvAdapter = new GridViewAdapter(this);
        gv.setAdapter(gvAdapter);
        if(gvAdapter.isEmpty()){
            noScreenshot.setVisibility(View.VISIBLE);
            selectScreenshot.setVisibility(View.GONE);
        }else{
            noScreenshot.setVisibility(View.GONE);
            selectScreenshot.setVisibility(View.VISIBLE);
        }

        ActionBar bar = getSupportActionBar();
        Window window = getWindow();
        setActionBarColour(bar, window, this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        // Do OCR engine initialization, if necessary
        initOcrIfNecessary(this);
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
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if(clipboard.getPrimaryClip() == null){
            Toast.makeText(getApplicationContext(),
                    getString(R.string.empty_clipboard_toast),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
        if(item == null){
            Toast.makeText(getApplicationContext(),
                    getString(R.string.empty_clipboard_toast),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String pasteData = item.getText().toString();

        String excerpt = "";
        if(pasteData != null){
            excerpt = pasteData.trim();
        }

        if(excerpt.length() <= 0){
            Toast.makeText(getApplicationContext(),
                    getString(R.string.empty_clipboard_toast),
                    Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (isNetworkAvailable(getApplicationContext())) {
            Intent intent = new Intent(this, CustomizeActivity.class);
            intent.setAction(Intent.ACTION_DEFAULT);
            intent.putExtra(EXCERPT, excerpt);
            startActivity(intent);
        } else {
            CharSequence text = getString(R.string.no_internet_error);
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
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
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                float ratio = ((float) size.y) / size.x;

                view = new AspectRatioImageView(context, ratio);
                view.setScaleType(CENTER_CROP);
            }

            // Get the image URL for the current position.
            final Uri source = getItem(position);

            Picasso.with(context) //
                    .load(source) //
                    .placeholder(R.color.tw__light_gray) // placeholder
                    .error(R.color.material_blue_grey_800) // error
                    .fit() //
                    .centerCrop()
                    .tag(context) //
                    .into(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, CropActivity.class);
                    intent.putExtra(IMAGE, source.toString());
                    startActivity(intent);
                }
            });

            return view;
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
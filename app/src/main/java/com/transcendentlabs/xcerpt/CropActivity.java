package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.ArrayList;

import static com.transcendentlabs.xcerpt.Util.EXCERPT;
import static com.transcendentlabs.xcerpt.Util.getStorageDirectory;
import static com.transcendentlabs.xcerpt.Util.initOcrIfNecessary;
import static com.transcendentlabs.xcerpt.Util.setActionBarColour;

public class CropActivity extends AppCompatActivity {

    CropImageView cropImageView;
    private static final String SHOW_HINT_SETTING = "hint";
    private ArrayList<AsyncTask> tasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        cropImageView = (CropImageView) findViewById(R.id.CropImageView);
        tasks = new ArrayList<>();

        cropImageView.setGuidelines(0);

        Bundle extras = getIntent().getExtras();
        Uri uri = Uri.parse(extras.getString(InputActivity.IMAGE));
        try {
            Bitmap img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            cropImageView.setImageBitmap(img);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ActionBar bar = getSupportActionBar();
        Window window = getWindow();
        setActionBarColour(bar, window, this);

        SharedPreferences settings = getPreferences(0);
        showGuide(settings);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for(AsyncTask task : tasks){
            task.cancel(true);
        }
    }

    private void showGuide(final SharedPreferences settings) {
        boolean showHint = settings.getBoolean(SHOW_HINT_SETTING, true);

        if(showHint) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(getString(R.string.crop_instructions));
            builder.setMessage(getString(R.string.crop_instructions_2));
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(SHOW_HINT_SETTING, false);
                    editor.commit();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_crop, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_crop) {
            Bitmap finalImage = cropImageView.getCroppedImage();
            if(finalImage == null){
                Toast.makeText(getApplicationContext(),
                        "Error: Crop failed.",
                        Toast.LENGTH_SHORT).show();
            }
            final Activity activity = this;
            initOcrIfNecessary(this);
            String dir = getStorageDirectory(this).toString();

            OcrAsyncTask ocrTask =
                    new OcrAsyncTask(this, finalImage, dir, new OcrAsyncTask.Callback() {
                @Override
                public void onComplete(Object o, Error error) {
                    if (error != null) {
                        Log.e("OcrAsyncTask", error.getMessage());
                        return;
                    }
                    String excerpt = (String) o;

                    if(excerpt.isEmpty() || isGibberish(excerpt)){
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                        builder.setTitle(getString(R.string.crop_error));
                        builder.setMessage(getString(R.string.crop_instructions_2));
                        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                        AlertDialog dialog = builder.create();
                        try{
                            dialog.show();
                        }catch(Exception ignored){

                        }
                    }else if (App.getInstance().isNetworkAvailable()) {
                        Intent intent = new Intent(activity, CustomizeActivity.class);
                        intent.setAction(Intent.ACTION_DEFAULT);
                        intent.putExtra(EXCERPT, excerpt);
                        startActivity(intent);
                    } else {
                        CharSequence text = getString(R.string.no_internet_error);
                        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            tasks.add(ocrTask);
            ocrTask.execute();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // this function won't be necessary when we use the actual image
    private boolean isGibberish(String text){
        // strategy: see if the amount of non alphanumeric characters is too high for actual text
        // may want to make this strategy smarter in the future

        int numAlphanumericChars = text.replaceAll("[^a-zA-Z ]", "").length();
        double ratio = (double) numAlphanumericChars / text.length();
        Log.e("isGibberish", "Ratio: " + ratio);
        return ratio < 0.75;
    }
}

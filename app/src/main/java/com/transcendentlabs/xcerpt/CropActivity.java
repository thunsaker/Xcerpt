package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.util.ArrayList;

import static com.transcendentlabs.xcerpt.Util.EXCERPT;
import static com.transcendentlabs.xcerpt.Util.getStorageDirectory;
import static com.transcendentlabs.xcerpt.Util.initOcrIfNecessary;

public class CropActivity extends BaseActivity {

    CropImageView cropImageView;
    private static final String SHOW_HINT_SETTING = "hint";
    private ArrayList<AsyncTask> tasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tasks = new ArrayList<>();
        setContentView(R.layout.activity_crop);

        if (!initializeCropView()) {
            return;
        }

        setActionBarElevationOff();

        SharedPreferences settings = getPreferences(0);
        showGuide(settings);
    }

    // return true if initialization successful
    private boolean initializeCropView() {
        cropImageView = (CropImageView) findViewById(R.id.CropImageView);
        cropImageView.setGuidelines(0);
        Intent receivedIntent = getIntent();
        String action = receivedIntent.getAction();
        Bundle extras = receivedIntent.getExtras();
        Uri uri;
        if(Intent.ACTION_SEND.equals(action)) {
            uri = extras.getParcelable(Intent.EXTRA_STREAM);
        } else {
            uri = Uri.parse(extras.getString(InputActivity.IMAGE));
        }
        try {
            Bitmap img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

            if(img == null) {
                finish();
                return false;
            }
            cropImageView.setImageBitmap(img);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
            return false;
        }
        return true;
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
            displayDialog(dialog);
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
            pushCroppedImageToCustomize();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void pushCroppedImageToCustomize() {
        Bitmap finalImage = cropImageView.getCroppedImage();
        if(finalImage == null){
            Toast.makeText(getApplicationContext(),
                    "Error: Crop failed.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        initOcrIfNecessary(this);
        String dir = getStorageDirectory(this).toString();

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.processing_image));
        dialog.setCancelable(false);

        final OcrAsyncTask ocrTask =
                new OcrAsyncTask(finalImage, dir, new OcrAsyncTask.Callback() {
            @Override
            public void onComplete(Object o, Error error) {
                onOcrComplete((String) o, error);
            }
        });
        tasks.add(ocrTask);
        ocrTask.execute();

        displayOcrTaskDialogWithTimeout(dialog, ocrTask);
    }

    private void displayOcrTaskDialogWithTimeout(ProgressDialog dialog, final OcrAsyncTask ocrTask) {
        displayDialog(dialog);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(ocrTask.getStatus() == AsyncTask.Status.RUNNING) {
                    ocrTask.cancel(true);
                    showErrorDialog(CropActivity.this);
                }
            }
        }, 8000);
    }

    private void onOcrComplete(String excerpt, Error error) {
        closeDialog();
        if (error != null) {
            Log.e("OcrAsyncTask", error.getMessage());
            return;
        }

        if(isGibberish(excerpt)){
            showErrorDialog(this);
        }else if (App.getInstance().isNetworkAvailable()) {
            pushToCustomizePage(excerpt);
        } else {
            showNoInternetErrorToast();
        }
    }

    private void showNoInternetErrorToast() {
        Toast.makeText(getApplicationContext(),
                getString(R.string.no_internet_error),
                Toast.LENGTH_SHORT)
            .show();
    }

    private void pushToCustomizePage(String excerpt) {
        Intent intent = new Intent(this, CustomizeActivity.class);
        intent.setAction(Intent.ACTION_DEFAULT);
        intent.putExtra(EXCERPT, excerpt);
        Bundle extras = getIntent().getExtras();
        intent.putExtra(InputActivity.IMAGE, extras.getString(InputActivity.IMAGE));
        startActivity(intent);
    }

    private void showErrorDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(getString(R.string.crop_error));
        builder.setMessage(getString(R.string.crop_instructions_2));
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog dialog = builder.create();
        displayDialog(dialog);
    }

    // this function won't be necessary when we use the actual image
    private boolean isGibberish(String text){
        if(text == null || text.isEmpty()) {
            return true;
        }
        // strategy: see if the amount of non alphanumeric characters is too high for actual text
        // may want to make this strategy smarter in the future
        int numAlphanumericChars = text.replaceAll("[^a-zA-Z ]", "").length();
        double ratio = (double) numAlphanumericChars / text.length();
        Log.e("isGibberish", "Ratio: " + ratio);
        return ratio < 0.75;
    }
}

package com.transcendentlabs.xcerpt;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.transcendentlabs.xcerpt.Util.EXCERPT;
import static com.transcendentlabs.xcerpt.Util.isNetworkAvailable;
import static com.transcendentlabs.xcerpt.Util.setActionBarColour;

public class CropActivity extends AppCompatActivity {

    CropImageView cropImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        cropImageView = (CropImageView) findViewById(R.id.CropImageView);

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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_crop, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_crop) {
            Bitmap finalImage = cropImageView.getCroppedImage();

            String datapath = Environment.getExternalStorageDirectory() + "/Xcerpt/";
            String language = "eng";
            File dir = new File(datapath + "tessdata/");
            if (!dir.exists()){
                dir.mkdirs();
                File dataFile = new File(dir + language + ".traineddata");
                if(!dataFile.exists()){
                    try {
                        AssetManager am = getAssets();
                        String[] list = am.list("");
                        for (String s:list) {
                            if (s.endsWith("traineddata")) {
                                Log.d("TessOCR", "Copying asset file " + s);
                                InputStream inStream = am.open(s);
                                int size = inStream.available();
                                byte[] buffer = new byte[size];
                                inStream.read(buffer);
                                inStream.close();
                                FileOutputStream fos = new FileOutputStream(dir + "/" + s);
                                fos.write(buffer);
                                fos.close();
                            }
                        }
                    }
                    catch (Exception e) {
                        // Better to handle specific exceptions such as IOException etc
                        // as this is just a catch-all
                    }
                }
            }

            final Activity activity = this;

            OcrAsyncTask ocrTask = new OcrAsyncTask(this, finalImage, new OcrAsyncTask.Callback() {
                @Override
                public void onComplete(Object o, Error error) {
                    if (error != null) {
                        Log.e("OcrAsyncTask", error.getMessage());
                        return;
                    }
                    String excerpt = (String) o;

                    if (isNetworkAvailable(getApplicationContext())) {
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

            ocrTask.execute();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}

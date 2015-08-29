package com.transcendentlabs.xcerpt;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.InputStream;

import static com.transcendentlabs.xcerpt.Util.*;

public class CropActivity extends AppCompatActivity {

    CropImageView cropImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        cropImageView = (CropImageView) findViewById(R.id.CropImageView);

        cropImageView.setGuidelines(0);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray(CustomizeActivity.IMAGE);
        Bitmap img = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        cropImageView.setImageBitmap(img);

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

            TessOCR tesseract = new TessOCR();

            ProgressDialog progress;
            progress = ProgressDialog.show(this, "Processing image...",
                    "dialog message", true);

            String excerpt = tesseract.getOCRResult(finalImage);
            excerpt = excerpt.replaceAll("\n", " ");
            excerpt = excerpt.replaceAll("  ", "\n\n");
            excerpt = excerpt.replaceAll("\\p{Pd}", "-");

            progress.dismiss();

            if (isNetworkAvailable(getApplicationContext())) {
                Intent intent = new Intent(this, CustomizeActivity.class);
                intent.setAction(Intent.ACTION_DEFAULT);
                intent.putExtra(EXCERPT, excerpt);
                startActivity(intent);
            } else {
                CharSequence text = getString(R.string.no_internet_error);
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}

package com.transcendentlabs.xcerpt;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.isseiaoki.simplecropview.CropImageView;


public class CropActivity extends AppCompatActivity {

    private Bitmap img;
    private CropImageView cropImage;

    public static final String EXCERPT = "com.transcendentlabs.xcerpt.excerpt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);

        Bundle extras = getIntent().getExtras();
        byte[] byteArray = extras.getByteArray(CustomizeActivity.IMAGE);
        img = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        cropImage = (CropImageView) findViewById(R.id.cropImageView);
        cropImage.setImageBitmap(img);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_crop, menu);

        MenuItem cropItem = menu.getItem(0);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.crop) {
            // TODO read text, send text to CustomizeActivity
            Bitmap croppedImage = cropImage.getCroppedBitmap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
